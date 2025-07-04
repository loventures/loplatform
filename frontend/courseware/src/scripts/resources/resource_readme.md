## Resources as a hybrid loader paradigm

How we get to a sane future.

### What we have:

There exist three previous forms of resource loading in course-lw.

- The first is the **AngularJS api services**. They are called from both Angular components and from
  Loader Components. They have the advantage of using Request and $http to force a
  digest cycle when promises resolve in order to update the angular views.
- The **Loader Component** framework is a series of
  abstractions that build React components that fetch a resource, transform the
  data, and store it in Redux. The child component is not rendered until loading is
  complete but it also trusts that arbitrary data is available from the store upon
  rendering. Many of these Loader Components use $injector to call out to AngularJS
  services to perform the loading request.
- The **Loadable Components** are TypeScript utilities that load data before rendering
  any children. The loaded data is passed through to the children via render props,
  making compatibility with other loaders difficult. These loaders often store data
  in Redux as well. Their primitive type is a Loadable which are monadic tagged
  type classes. Such functional.

### Resources are the future

Resources consist of a base TypeScript class which inherits WithStore
and implements the Resource interface. The goal is to provide a native JS/TS structure
that will supercede frameworks, bridge the gaps between all three existing loaders, and
provide an easy to understand paradigm for future development.

A resource has one main method: _read_. This method accepts params that act as cache keys
and returns an object containing a promise, the data, status of the promise and the key.  
After a load is complete the resource can also dispatch
the existing myriad of actions our current loaders produce. This is a stopgap measure to help
transition away from Loader Components and their extra confusing selectors.

The guts of how the requests are made/cached/invalidated/etc are handled by a library called
**react-query**. While the library is intended for use with hooks, it provides just enough
external surface area that we could build wrapper resources that can interact beyond just hooks.

Each resource is accompanied by a hook. This hook calls the read method and conforms to the suspense
API. It will throw the promise if the resource hasn't loaded, or it will return the data. Consumers
of these hooks should wrap themselves in a Suspense component.

Notes:
Some loaders do a lot of processing before pushing things to the store. We will still have
to do that. In those cases it may be best to have one resource consume another. See LearningPathResource.

### Unfinished business

- Angular components should be able to use the resource instance directly, but I haven't tried
  it out. We may need to pass in the _apply_ function in or trigger a _rootScope.apply_ after
  loading.
- Full API surface area is not fully accounted for in the resource pattern. We could add post/put
  methods to a resource. This will result in fleshing out how mutations/updates are handled for
  resources. For now, we should continue to use whatever exists and makes sense to send updates.
- Multiple api surfaces for complex behavior such as editing or adding sub-entities (qna questions vs messages)
- Caveat for updates: we will need to invalidate queries and refetch or mutate when sending updates.
- We are using **Suspense**. This is a powerful technique that works well. It throws away the render
  function if a promise is thrown. When the promise resolves, it tries to rerender. There are warnings
  about using Suspense because the API isn't fully fleshed out. But the basics work and what's what we
  need. We will need to take advantage of more advanced features such as useTransition soon, but that is
  also fine. It works too. The biggest rule to remember is that Refs can't be set during a suspendable
  render because the whole render is discarded. You must set refs outside of the render such as in a useEffect.
- If for some reason Suspense is a failure, the resource pattern can be quickly refactored to a typical
  no-render til data pattern. This pattern is just far less expressive since you end up passing data
  props or using render props (ugh).

### Prior Art

The **swr** and **react-query** libraries are the inspiration for this pattern. They have
advanced caching, updating, and performance optimizations. But they are "hook-centric". This runs
up against the reason why Loadable Components are not a good path forward: they aren't interoperable.
I started with an attempt to use swr but I was never able to get it to play nice. There was
no way for non-React.FC contexts to take advantage of these resources. Thus, I had to write my own
resource class and hooks to access them. When I investigated react-query I found it has a much larger
external surface area, and it could nicely fit inside the resource classes already built.
