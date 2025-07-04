/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package loi.cp.job.dataconsistency;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.job.AbstractJavaEmailJob;
import loi.cp.job.GeneratedReport;

import javax.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Boiler plate class for Data Consistency Jobs.
 */
public abstract class AbstractDataConsistencyJob<T extends DataConsistencyJob<T>> extends AbstractJavaEmailJob<T>
  implements DataConsistencyJob<T> {

    /** An enum for indicating whether to process the ids in ascending or descending order. */
    public enum Order {Ascending, Descending}

    private static final Logger logger = Logger.getLogger(AbstractDataConsistencyJob.class.getName());
    private static final int MAX_LOOPS = 100000; //If a task takes no time it can peg the CPU. This just stops it staying
                                                 //pegged for the whole time window allocated. It should be high enough
                                                 //that it doesn't interfere with normal operation but low enough that
                                                 //an empty loop of that size won't screw things for long

    /**
     * Whether to process ids in ascending or descending order.
     * If ascending will start at -1 and go to whatever the getQueryToDetermineNextBatchEndpoint() query returns (defaults to just the
     * max id in the table).
     * If descending will start at whatever getQueryToDetermineNextBatchEndpoint() returns and will go to -1.
     */
    private Order order;

    @Inject
    private QueryService _queryService;

    public AbstractDataConsistencyJob() {
        order = Order.Ascending;
    }

    public AbstractDataConsistencyJob(Order order) {
        this.order = order;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataConsistencyJobConfig getConfig() {
        return (DataConsistencyJobConfig) self().getConfig();
    }

    @Override
    public GeneratedReport generateReport() {
        DataConsistencyJobConfig config = getConfig();

        //Only needs to be run when initially creating the job.  Persists a list of tableConsistencyTasks.
        if (config.tableConsistencyTasks == null || config.tableConsistencyTasks.isEmpty()) {
            config.tableConsistencyTasks = createTableTasks(getQueryInfoMap());
        }
        String name = StringUtils.defaultIfBlank(getName(), getClass().getSimpleName());
        UploadInfo csv = executeConsistencyTasks(name + ".csv", config);
        self().setConfig(config);

        StringBuilder body = new StringBuilder(getName()).append("\n\n");
        for (TableConsistencyTask task : config.tableConsistencyTasks) {
            body.append(String.format("Max id in %s: %d\n", task.itemTypeOrTableName, task.lastProcessedId));
        }

        return new GeneratedReport(name, body.toString(), csv);
    }

    //How to handle each ConsistencyTask.  Can be Overridden for specific needs / reports.
    private UploadInfo executeConsistencyTasks(String filename, DataConsistencyJobConfig config) {
        UploadInfo ui = csvFile(filename);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(ui.getFile()), StandardCharsets.UTF_8);
            CSVWriter cw = new CSVWriter(writer)) {
            cw.writeNext(csvHeader());

            HashSet<TableConsistencyTask> remainingTasks = new HashSet<>(config.tableConsistencyTasks);
            //Repeat non-completed tasks for a set amount of time.
            long startTime = System.currentTimeMillis();
            int loop = 0;
            while (System.currentTimeMillis() - startTime < config.timeLimitInSeconds * 1000 && !remainingTasks.isEmpty() && loop++ < MAX_LOOPS) {
                for(TableConsistencyTask activeTask: new HashSet<>(remainingTasks)) {
                    cw.writeAll(runTaskAndGetCsvLines(activeTask));
                    cw.flush();
                    if (activeTask.finished) {
                        remainingTasks.remove(activeTask);
                    }
                }
            }

            //Only print completed tasks one time, at the end of the file.  Prevents clutter.
            for (TableConsistencyTask task : config.tableConsistencyTasks) {
                if (task.finished) {
                    cw.writeNext(getCsvLineForCompletedTask(task));
                    cw.flush();
                }
            }

            if (config.runForever) {
                boolean allFinished = config.tableConsistencyTasks.stream().allMatch(task -> task.finished);
                if (allFinished) {
                    config.tableConsistencyTasks.stream().forEach(task -> {
                        task.lastProcessedId = -1L;
                        task.finished = false;
                    });
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return ui;
    }

    protected List<String[]> runTaskAndGetCsvLines(TableConsistencyTask task) {
        //prevMaxID is used as the minimum ID for the upcoming query.
        String minId = task.lastProcessedId.toString();
        //runConsistencyTask(task) updates lastProcessedId to its new value.  Report again for an ID range.
        Stopwatch timer = Stopwatch.createStarted();
        String[] result = runConsistencyTask(task);
        String maxId = task.lastProcessedId.toString();

        return Collections.singletonList(
          new String[] {
            task.itemTypeOrTableName,
            task.sql,
            minId,
            maxId,
            result[0],
            timer.toString()
          }
        );
    }

    private String[] getCsvLineForCompletedTask(TableConsistencyTask task) {
        return new String[]{
          task.itemTypeOrTableName,
          task.sql,
          task.lastProcessedId.toString(),
          task.lastProcessedId.toString(),
          "Task Completed on Previous Run. Max ID: " + task.lastProcessedId
        };
    }

    private String[] csvHeader() {
        return new String[]{
          "Item Type",
          "Task SQL",
          "Min ID",
          "Max ID",
          "Query Result",
          "Time"
        };
    }

    //TableConsistencyTasks created using getQueryInfoMap().
    private List<TableConsistencyTask> createTableTasks(Map<String, List<String>> tableUpdateInfo) {
        ArrayList<TableConsistencyTask> taskList = new ArrayList<>();

        for (String itemType : tableUpdateInfo.keySet()) {
            for (String sqlStatement : tableUpdateInfo.get(itemType)) {
                TableConsistencyTask task = new TableConsistencyTask();
                task.sql = sqlStatement;
                task.itemTypeOrTableName = itemType;
                if (getConfig().initialMaxId != null) {
                    task.lastProcessedId = getConfig().initialMaxId;
                }
                taskList.add(task);
            }
        }

        return taskList;
    }

    //Run the update query.  Returns rows updated, updates complete, error, etc.
    protected String[] runConsistencyTask(TableConsistencyTask task) {
        Stopwatch timer = Stopwatch.createStarted();
        Query q = createQueryFromTask(task);
        try {
            Long lastIdInBatch = (Long) q.getParameterValue(order == Order.Ascending ? "maxId" : "minId");
            boolean complete = (order == Order.Ascending ?
              task.lastProcessedId >= lastIdInBatch :
              task.lastProcessedId <= lastIdInBatch);
            if (complete) {
                task.finished = true;
                String finishedMessage = String.format("Updates Complete! Last ID Updated %d", task.lastProcessedId);
                logger.info(finishedMessage);
                return new String[] {finishedMessage};
            }
            String[] queryResult = runQuery(q);

            //Commit changes.  Start a new Transaction.
            ManagedUtils.commit(true);

            //To avoid skipping data, only update maxId once.
            task.lastProcessedId = lastIdInBatch;

            return queryResult;
        } catch (PersistenceException e) {
            logger.log(Level.WARNING, String.format("Data Consistency failed! Job ID: %d, Last Processed ID: %d, Failed SQL: %s (%s)",
              self().getId(), task.lastProcessedId, task.sql, timer.toString()), e);

            //Roll back changes.  Start a new transaction.
            ManagedUtils.commit(false);
            return new String[] {getAllExceptions(e, 0)};
        } finally {
            EntityContext.flushAndClearCaches();
        }
    }

     /**
      *  Run the actual query.  By default the return value is just a a number representing rows updated.
      */
    protected String[] runQuery(Query q) {
        Integer rowsUpdated =  q.executeUpdate();
        return new String[] {String.format("Rows Updated: %d", rowsUpdated)};
    }

    //Throwables should not have themselves as children/grandchildren/etc... but it's possible
    private String getAllExceptions(Throwable e, int infiniteRecursionSanityCheck) {
        if (e.getCause() == null || infiniteRecursionSanityCheck > 10) {
            return e.getMessage();
        }

        return e.getMessage() + ", " + getAllExceptions(e.getCause(), infiniteRecursionSanityCheck + 1);
    }

    //Create and return a query without running it.
    private Query createQueryFromTask(TableConsistencyTask task) {
        Query query = _queryService.createNativeQuery(task.sql);
        if (order == Order.Ascending) {
            query.setParameter("minId", task.lastProcessedId);
            query.setParameter("maxId", nextBatchEndpoint(task));
        } else {
            if (task.lastProcessedId == -1) {
                task.lastProcessedId = Long.MAX_VALUE;
            }
            query.setParameter("minId", nextBatchEndpoint(task));
            query.setParameter("maxId", task.lastProcessedId);
        }

        return query;
    }

    //Get the new Maximum ID.  Decided by batch size of the task, and previousMax.
    @SuppressWarnings("unchecked")
    private Long nextBatchEndpoint(TableConsistencyTask task) {
        String tableName = getTableName(task);
        String queryString = getQueryToDetermineNextBatchEndpoint(task, tableName);

        logger.log(Level.INFO, queryString);
        Query q = _queryService.createNativeQuery(queryString);

        List<Object> resultList = q.getResultList();
        if ((resultList != null) && (resultList.get(0) != null)) {
            Number endId = (Number) resultList.get(0);
            return endId.longValue();
        }
        return task.lastProcessedId;
    }

    private String getTableName(TableConsistencyTask task) {
        EntityDescriptor finder = BaseOntology.getOntology().getEntityDescriptor(task.itemTypeOrTableName);
        String tableName;
        if (finder == null) {
            //Item type unavailable.  Assume string literal table name has been supplied.
            tableName = task.itemTypeOrTableName;
        } else {
            tableName = finder.getTableName();
        }
        return tableName;
    }

    /**
     * Generates a query that will yield the last id in the next batch of ids to run.
     * This can be overridden by derived classes that do not want to iterate over all
     * the rows in the table, though that is really only necessary if the consistency tasks
     * main SQL statement is an update.  If overridden, the query must return a single number.
     */
    protected String getQueryToDetermineNextBatchEndpoint(TableConsistencyTask task, String tableName) {
        // For ascending should look like
        // SELECT MAX(id) FROM (SELECT id FROM mytable WHERE id > 10283438 ORDER BY id ASC LIMIT 20) AS endID
        // For descending should look like
        // SELECT MIN(id) FROM (SELECT id FROM mytable WHERE id < 10283438 ORDER BY id DESC LIMIT 20) AS endID

        String queryString = "SELECT " + (order==Order.Ascending ? "MAX" : "MIN") + "(id) FROM(";
        queryString += " SELECT id from " + tableName;
        queryString += " WHERE id " + (order==Order.Ascending ? ">" : "<") + " " + task.lastProcessedId;
        queryString += " ORDER BY id " + (order==Order.Ascending ? "ASC" : "DESC");
        queryString += " LIMIT " + getConfig().batchSizeLimit + ")";
        queryString += " AS nextID";
        return queryString;
    }

    @Override
    public T update(T job) {
        /*
        Job is coming back from the server as JSON with empty tableConsistencyTasks.
        Need to remind it what those tasks are before calling super.update(..).
         */
        if (getConfig() != null) {
            job.getConfig().tableConsistencyTasks = getConfig().tableConsistencyTasks;
        }
        self().setConfig(job.getConfig());
        return super.update(job);
    }
}
