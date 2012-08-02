## gsmith-eclipse

Some eclipse plugins I wrote that do things I found myself needing:

* Toggle read/write state command for files
* A decorator for readonly files 
* Setting the eclipse window size
* Thumbnail decorator for image files
* A simple image viewer
* Screenshots
* A scratch pad text view

## Build

For now, you'll need to build these in Eclipse itself. Just _git pull_, 
Import the projects into your workspace, and the Export the
_gsmith.eclipse.ui.feature_ project as a deployable feature.

The plugins work back to Eclipse 3.6 intentionally, since I still have to use
that for some projects I work on.

## License

See the [LICENSE](https://github.com/smithgp/gsmith-eclipse/README.md) file for details.
