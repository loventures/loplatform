High-level react tree:

```
.CourseCustomizer
  .CustomizationContentTree
    .CustomizerHeader
      .CheckoutModal
        .DiffView
      .ResetModal
    .RenderContentTree
      .ContentNode
      .Target
```

1. `CourseCustomizer` fetches the CustomisableContents and shows a loading screen.
2. `CustomizationContentTree` connects the customizer state.
3. `CustomizerHeader` shows the undo/redo/reset buttons & others
4. `CheckoutModal` shows the errors or the diff of what the instructor will commit.
5. `DiffView` shows the diff of what the instructor has edited
6. `ResetModal` shows the content that has changes which will be reverted when submitted.
7. `RenderContentTree` is a recursive tree that displays the editable course contents
8. `ContentNode` A single node in a tree. This encapsulates all the edit inputs.
9. `Target` A droppable area where a ContentNode can be dropped on.

High-Level state shape (`customizationsReducer`):

```
{
  courseCustomizations: {
    customisations: Tree<CustomisableContent>
    customizerState: {
      collapsedContent: string[];
      hiddenItemsVisible: boolean;
      currentDraggingContext?: {
        dragging: string;
        parent: string;
      };
      edits: ContentEdit[];
      redoStack: ContentEdit[];
    }
  }
}
```

1. `customisations` is the tree of customizations that comes from the server.
2. `collapsedContent` a list of all content-nodes that are "collapsed," meaning, their children should be hidden.
3. `hiddenItemsVisible` - whether or not hidden items should be shown.
4. `currentDraggingContext` - The currently dragging node
5. `edits` - a list of all the edits the instructor has currently made.
6. `redoStack` - a list of items that should be redone (in reverse order).
