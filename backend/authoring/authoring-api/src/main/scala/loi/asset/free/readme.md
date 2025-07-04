
# Building asset trees for unit/db/int tests

Building an asset tree is done in 3 steps:

1. Constructing the instruction graph set.
2. Translating the Instruction set with the appropriate (unit/db/int) interpreter.
3. Running the translated instruction set. 


## Building the instruction graph set
Constructing the instruction set is the same for all environments. Note that when building this set of instructions, you're not actually creating assets/edges. You're merely building the instructions that you'll eventually send off to the authoring environment, which will create them.

You can create assets with the `add` method, and add edges with the `associate` method:

```scala 
import loi.asset.free.AssetGraphInstruction.{add, associate}

for {
  course  <- add(CourseData("Test Course"))
  
  module <- add(ModuleData("Module 1"))
  edge1   <- associate(course, module, Group.Elements)
  
  lesson <- add(LessonData("Lesson 1"))
  edge2   <- associate(module, lesson, Group.Elements)
} yield course
```

Here we're creating a course, module, and lesson, and creating edges between them by associating them.

The `Elements` group is commonly used, so there's an enrichment method for it, called `to`, which is defined on instances of `AssetGraphInstructionProgram[Asset[_]]`:

```scala
import loi.asset.free.AssetGraphInstruction.{add, associate}

for {
  course <- add(CourseData("Test Course"))
  module <- add(ModuleData("Module 1")) to course
  lesson <- add(LessonData("Lesson 1")) to module
} yield course
```

The type of the resulting expressing is `AssetGraphInstructionProgram[A]` (where `A` is the type of the value you yielded from your for comprehension. This value represents the sequence of instructions that is required to construct an `A`.

```scala
val tree: AssetGraphInstruction[Course] = 
  for {
    course <- add(CourseData("Test Course"))
    ...
  } yield course
```

In order to put this instruction sequence to use, we must first translate it, using the interpreter that's built for our environment.


## Unit Test interpreter.

The unit test interpreter constructs a glorified map of assets in memory which you can use to test your apis.

If you have an `AssetGraphInstructionProgram[A]`, you can use the `InMemoryAssetExecutor.interpreter` to translate this into a set of assets:
 
```scala 
import loi.asset.free.AssetGraphInstruction.{add, associate}
import loi.cp.content.{InMemoryAssetExecutor, InMemoryAssetStore}

val tree = for {
  course <- add(CourseData("Test Course"))
  module <- add(ModuleData("Module 1")) to course
  lesson <- add(LessonData("Lesson 1")) to module
} yield course

val executor = tree.foldMap(InMemoryAssetExecutor.interpreter)

val (populatedStore, course) = executor(InMemoryAssetStore.empty)
```

`executor` is a function that takes the glorified database of assets and populates it, returning the populated version. Most of the time, you just want to supply an empty store. 

## Db Test interpreter.

The Db test interpreter creates assets in the db test environment (typically `aboveground`). You must have access to an instance of DbAssetExecutorDeps

```scala
import loi.asset.free.AssetGraphInstruction.add
import loi.authoring.{DbAssetExecutor, DbAssetExecutorDeps}

val tree = for {
  course <- add(CourseData("Test Course"))
  module <- add(ModuleData("Module 1")) to course
  lesson <- add(LessonData("Lesson 1")) to module
} yield course

val (course) = doAuthoringTxn { app =>
  var ws = app.spawnWriteWorkspace("test-branch")

  val deps = DbAssetExecutorDeps(app, new Date())

  val (ws1, course) = tree.foldMap(DbAssetExecutor.interpreter).run(deps).apply(ws).unsafePerformIO()
  course
}
```

## Integration test interpreter.

The integration test interpreter creates assets via the web api. You must have an instance of `RemoteAssetExecutorDeps`, which is basically just a wrapper around `DeWebClient`:

```scala
import loi.asset.free.AssetGraphInstruction.add
import loi.cp.test.{RemoteAssetExecutor, RemoteAssetExecutorDeps}

val tree = for {
  course <- add(CourseData("Test Course"))
  module <- add(ModuleData("Module 1")) to course
  lesson <- add(LessonData("Lesson 1")) to module
} yield course

val deleter = new AssetWebResourceDeleter(new BaseWebResourceDeleter)
val authoringApi  = new AuthoringWebApi(deWebClient, deleter)
val RemoteResult(project, branch, course) =
  assetGraph.foldMap(RemoteAssetExecutor.interpreter)
    .withProject("it-course", "Course")(authoringApi).unsafePerformIO()
```
