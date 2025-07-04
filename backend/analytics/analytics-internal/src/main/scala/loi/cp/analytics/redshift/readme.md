# Redshift ETL
This package extracts some data from our analytic events, and loads it into
an Amazon Redshift database. Clients can be given read access to this database
and use it for student progress and risk reporting.

The data sent to Redshift was dedicated to client's exact needs. No effort was 
made to create a generic event stream for any client to answer any question. 

## Data Flow
1. Business transactions save events in `analyticfinder` (in the same 
transaction).
2. A background thread, `AnalyticsPoller-0` grabs chunks of 
`analyticfinder`, and invokes the active `AnalyticBus` instances on the
window of events. The buses are managed in the *Analytic Buses* area of the
Administration app. Each active bus will receive each window. The `RedshiftEventSender` 
is an `AnalyticBus` that performs this ETL (extract, transfer, load).
4. The sender extracts student progress and risk reporting data from the window 
of events.
5. The sender serializes this data into JSON and writes the JSON documents into files.
6. The sender sends the files to an S3 bucket. 
7. The sender connects to Redshift, and issues `COPY` commands to load files from S3 
into Redshift tables. 
8. The sender deletes the S3 files. 

## Errors
When the sender has an error, the `AnalyticsPoller-0` thread will retry the window 
of events after some duration. Failing events are retried indefinitely. New events,
while enqueued, are blocked by the failing events to maintain ordering. Failures 
must be unblocked by human intervention to allow new events to process.

## State
Many of the Redshift tables for this report represent state, not events. For
example, if the sender receives an event that describes a course section, the
sender will query the `section` Redshift table to determine if the course 
section is new, updates and existing Redshift record, or is exactly the same as
the existing Redshift record. In another example, grades can be unset, and the 
sender deletes Redshift records. 
 
## Local Setup
To create a sender or run the Redshift tests locally, you will need to create a personal
database user and database in a nonprod cluster in the AWS Dev account.

### Create Database
1. Go to Amazon Redshift in the AWS Dev account.
1. Open the Query Editor.
1. Connect to the `nonprod` cluster, `dev` database, `awsuser` user, and **Connect with temporary password**.
1. Run `create user sjordan password 'pw' createdb` but replace pw and sjordan of course.
1. Run `create database sjordandb owner sjordan` but replace sjordan of course.

### Configure Intellij
1. Open the Databases panel in Intellij and add a new **Data Source > Amazon Redshift** connection.
1. Name it `lo-dev/nonprod`.
1. Paste the JDBC URL of the cluster and edit the database name to `sjordandb`. The JDBC url
is available on the properties page of the Redshift cluster in the AWS console but it will have `defualtdb` as its database, 
which is unused by us.
1. Choose **User & Password** authentication and enter your username and password from above.
1. Go to the **Options** panel and select **Read Only** (or don't).
1. Go to the **SSH/SSL** panel and check **Use SSH Tunnel**. The host is `vpc.learningobjects.com`. Choose **Key pair** authentication.
1. Return to the General tab and choose **Test Connection**.

### Configure DE
Intellij connections can use an SSH tunnel but the DE application cannot. You will need
to edit the security group of the Redshift cluster to allow your current IP address to connect.

1. Navigate to the Properties page of the nonprod cluster in the AWS conosle.
1. Scroll to **Network and Security > VPC Security Group**
1. Click the security group.
1. Scroll to **Inbound Rules**.
1. Choose **Edit Inbound Rules**.
1. Add a rule for **Redshift**, choose **My IP** and add a comment like `sjordan home` or `wework`.

The DE connection details are kept in Typesafe Config. Unlike Intellij, DE inserts data using
S3 which is why there is S3 configuration.

1. Copy `analytics-internal/.../sample-user.conf` to `user.conf` in the same 
directory. 
1. Provide the URLs and credentials of your allocated resources. `user.conf` is 
git ignored. 

    | Property | Value                                                                        |
    | -------- |------------------------------------------------------------------------------|
    | de.databases.redshift.datasource.url | JDBC url above                                                               |
    | de.databases.redshift.datasource.user | Redshift user created above                                                  |
    | de.databases.redshift.datasource.pass | Redshift user pw created above                                               |
    | loi.cp.analytics.redshift.s3.prefix | like `sjordan/redshift-transfer`                                             |
    | loi.cp.analytics.redshift.s3.bucket | `lo-rs-dev-analytics`                                                        |
    | loi.cp.analytics.redshift.s3.role | `arn:aws:iam::12345:role/nonprod-redshift-001-RedshiftIamRole-1B48QIZ40T7U9` |
    | loi.cp.analytics.redshift.s3.credentials | An Access Key for your AWS Dev account                                       |

    If you do not already have an Access Key for your user in the AWS Dev account, you
    can create one in IAM. Search for your user and navigate to Security Credentials.
 
1. In SBT run `analyticsInternal/Redshifttest/testOnly loi.cp.analytics.redshift.RedshiftEventSenderRedshiftTest` (or 
run it from Intellij) to verify that all of your AWS resources are working.
1. (Optional)  Create the `AnalyticBus`. This step is optional because it is not required 
to run the Redshift tests locally. It is required if  you want your application's emitted
events to be sent to Redshift. Run this from sys/script in the domain where you want to emit data:
    ```scala
    summon("RedshiftStartupService").initializeEtl("cbllocal")
    ```
    You now have an active analytic bus that will periodically send data to a schema "cbllocal".
    If you leave this bus unpaused you are needlessly populating data in Redshift.
    
    A QA Automation Domain is already created with an active bus using schema "qa0".

## Lola Setup
Each Lola is given one database in the AWS Dev account's nonprod Redshift cluster.
The Lola's domain will send data to a schema named "qa0". See RedshiftStartupService.

## Upgrading Schema
Upgrading schema is a manual process. There is one schema per bus. Here is a sample
script that adds a table

```scala
import doobie.implicits._
import cats.syntax.functor._
import com.learningobjects.cpxp.scala.cpxp.Summon.summon
import loi.cp.analytics.redshift.RedshiftSchemaServiceImpl

val schemaService = summon[RedshiftSchemaServiceImpl]
schemaService.upgradeAll(sql"create table foo(id int8)".update.run.void)
```

## prod Redshift Permissions
There is a group `client_ro_group` that was given the grants below, where `loan` is 
a schema name that varies from domain to domain
```sql
grant usage on schema loan to group client_ro_group;
grant select on all tables in schema loan to group client_ro_group;
alter default privileges in schema loan grant select on tables to group client_ro_group;
```
