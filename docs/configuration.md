# Configuration
There are a few environment variables that can be set to control different aspects of the shelf. Since the shelf is not a stand-alone application, they must be set as environment variables or passed as arguments to whichever application uses the kgrid shelf as a dependency (in this document, the kgrid activator).
## Current Configuration

### `kgrid.shelf.cdostore.url`
- Specify the path to a custom shelf directory, which can be preloaded with KOs. Can be an absolute or relative path.
  - Default value: `shelf` (in current working directory)
  - Command line (absolute path):
    ```bash
    java -jar kgrid-activator-#.#.#.jar --kgrid.shelf.cdostore.url=filesystem:file:///data/myshelf
    ```
  - relative path:
    ```bash
    java -jar kgrid-activator-#.#.#.jar --kgrid.shelf.cdostore.url=filesystem:file:///c:/Users/me/myshelf
    ```
  - environment variable:
    ```bash
    export KGRID_SHELF_MANIFEST=filesystem:file:///data/myshelf
    ```

### `kgrid.shelf.endpoint`
- Specify a custom namespace for shelf endpoints.
  - Default value: `/kos`
  - Command line:
    ```bash
    java -jar kgrid-activator-#.#.#.jar --kgrid.shelf.endpoint=custom-shelf-endpoint
    ```
  - environment variable:
    ```bash
    export KGRID_SHELF_ENDPOINT=custom-shelf-endpoint
    ```
### `kgrid.shelf.expose.artifacts`
- Control whether artifacts within a KO can be accessed via the shelf api
  - Default value: `true`
  - Command line:
    ```bash
    java -jar kgrid-activator-#.#.#.jar --kgrid.shelf.expose.artifacts=true
    ```
  - environment variable:
    ```bash
    export KGRID_SHELF_EXPOSE_ARTIFACTS=true
    ```

### `kgrid.shelf.manifest` 
- Specify the path to a json file that contains a list of references to KOs that will be loaded on startup. Existing KOs in the shelf directory will be overwritten if they are contained in the manifest. This can be set to a file path, or a URL.
  - Default Value: none
  - Command line (file path):
    ```bash
    java -jar kgrid-activator-#.#.#.jar --kgrid.shelf.manifest=filesystem:file:///c:/Users/me/myStuff/manifest.json
    ```
  - Command line (URL):
    ```bash
    java -jar kgrid-activator-#.#.#.jar --kgrid.shelf.manifest=https://github.com/kgrid-objects/example-collection/releases/download/4.1.1/manifest.json
    ```
  - Environment variable (URL):
    ```bash
    export KGRID_SHELF_MANIFEST=filesystem:file:///c:/Users/me/myStuff/manifest.json
    ```