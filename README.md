# NuBank Challenge - Robots vs Dinosaurs

## Statement

**Robots vs Dinosaurs**
Nubank is assembling an army of remote-controlled robots to fight the dinosaurs and the first step towards that is to run simulations on how they will perform. You are tasked with implementing a service that provides a REST API to support those simulations.

These are the features required:
* Be able to create an empty simulation space - an empty 50 x 50 grid;
* Be able to create a robot in a certain position and facing direction;
* Be able to create a dinosaur in a certain position;
* Issue instructions to a robot - a robot can turn left, turn right, move forward, move backwards, and attack;
* A robot attack destroys dinosaurs around it (in front, to the left, to the right or behind);
* No need to worry about the dinosaurs - dinosaurs don't move;
* Display the simulation's current state;
* Two or more entities (robots or dinosaurs) cannot occupy the same position;
* Attempting to move a robot outside the simulation space is an invalid operation.

**Requirements**
* It must be written in a functional programming language (Clojure, Scala, Haskell or Elixir);
* You don't have to worry about databases; it's fine to use in-process, in-memory storage;
* It must be production quality according to your understanding of it: tests, logs, build file, README, etc.

**General notes**
* This challenge may be extended by you and a Nubank engineer on a different step of the process;
* Please make sure to anonymise your submission by removing your name from file headers, package names, and such;
* Feel free to expand your design in writing;
* You will submit the source code for your solution to us as a compressed file containing all the code and possible documentation. Please make sure to not include unnecessary files such as the Git repository, compiled binaries, etc;
* Please do not upload your solution to public repositories in GitHub, BitBucket, etc.
* Don't shy away from asking questions whenever you encounter a problem.

**Things we are looking for**
* Immutability/Referential transparency;
* Idiomatic code;
* Adherence to community/standard library style guides;
* Separation of concerns;
* Unit and integration tests;
* API design;
* Domain modelling;
* Attention to possible concurrency issues;
* Error handling.

## Notes from the developer

I enjoyed learning Clojure a lot, basically, I knew nothing about it, I just had had some exposure to Haskell and Erlang before, but I found Clojure to be substantially different. I decided not to write this project in Haskell (which would be the functional language in which I'm strongest) for various reasons, among others, to test if I would like to work with it for an extended period of time, to make the time to start coding as small as possible if I get to pass the selection process, and because Clojure seems to be pretty loved in its community and I wanted to know what all the excitement was about.

There are certainly many things that can be improved on this project, and I'll take a look at them in the future, since this is something that I want to have highlighted in my resume, but since the two allowed weeks have passed, I'm sending this version of the code (yes, sadly you will probably complain about my shallow testing T.T, but well).

In terms of technologies, I'm using `compojure-api` to do all the routing work and the swagger documentation, and I'm also testing with `midje`.

### Considerations

* The API assumes that the user is going to pass 1-indexed values, that means that robots and dinosaurs are numbered starting from 1, as also are grid coordinates.
* Internally, I'm using numbers from 0 to 3 to represent facing directions, where 0 means north, and it goes clockwise (1 means east, 2 means south, and 3 means west).
* The action "attack" is a `PUT` request to the resource `/simulations/{sid}/robots`, but in reality the resources updated are not the robots, but possibly, the dinosaurs (since some of the could get deleted). I don't really know if there is a right way to do this, I'm certainly intrigued.
* I'm using x and y meaning "first and second coordinates, respectively", please don't picture it as the mathematical axes (where greater x goes to the right, etc.), instead, imagine a two dimensional array, when first coordinate (that is, number of row) increases, you are going down, and when the second coordinate (that is, number of column) increases, you are going right.

### Things to improve

* Discover why the tests creating three dinosaurs are failing (this is a quite strange problem, I'm updating the atom representing the state with `swap!` and using its return value to get the value to send in the HTTP response, but somehow the three requests to create three different dinosaurs actually return the same).

* Fix the concurrency issue with the `valid?` function. Basically, I'm using `valid?` to check if the new position is a valid position, and only then I'm calling `swap!` to update the state, this is obviously wrong, since in the middle of those two calls the state could change again, and maybe the new position will no longer be valid. To fix this issue, I should use a `validator` that will check that there are no entities in the same position, and then I should wrap the `swap!` with the `try` and `catch` functions, capturing the `IllegalStateException`, if that occurs, I should send a response with a status code of `403`.

* Actually write those unit tests. I didn't write them since the state of my application is somewhat complicated, which I don't know if it is right in the first place (but to me there doesn't seem to be an obvious way to improve it).

* Improve the integrations tests. There are many situations still not covered. I should check things like moving off the board from every corner/side, making sure robots don't delete other robots when they attack, making sure you can't get into a cell occupied by a robot/dinosaur from any direction, making sure the display of the board is correct (probably testing that all the dinosaurs and robots that should be there are indeed there), and more than that, since this is a multidimensional problem, I should test things like adding a robot in the same cell, but in different simulations spaces (which should work), and I should check many related things to that (that robots don't delete dinosaurs from other simulation spaces, etc.). With this I hope to give a clear overview of how I would go about making the tests more thorough.

* The status code on newly created resources (simulation spaces, robots, and dinosaurs) should actually be `201 - Created`, but I'm just using `200` becuase if I wanted to use `201` I should also return the respective URL (this is not difficult, just takes a bit more of time).

* The swagger documentations displays the body of the requests as "Body14470" and "Body14471" and it doesn't provide useful default values (the default values for creating a new robot are in coordinates (0, 0), but these are not valid, since coordinates should be 1-indexed.

* (**out of scope**) Create a small client application to have a visual representation of how everything works.

## Usage

### Run the application locally

`lein ring server`

This opens up the server in `localhost` in the port `3000`. By default it will open the swagger documentation that you can use to manually test the API.

### Run the tests

`lein midje`

### API Endpoints

|  Method  |     Endpoint   | Description |
|----------|----------------|-------------|
| POST | /simulations        |   Creates a new empty 50 x 50 simulation space. |
| POST | /simulations/{sid}/robots | Creates a new robot in the defined simulation space, in the specified coordinates and with the required facing direction. |
| POST | /simulations/{sid}/dinosaurs | Creates a new dinosaur in the defined simulation space in the specified coordinates. |
| PUT  | /simulations/{sid}/robots/{rid}?action=turn-left | Makes a robot change its facing direction to the left.   |
| PUT  | /simulations/{sid}/robots/{rid}?action=turn-right | Makes a robot change its facing direction to the right. |
| PUT  | /simulations/{sid}/robots/{rid}?action=move-forward | Makes a robot change its position by moving forward according to its facing direction. |
| PUT  | /simulations/{sid}/robots/{rid}?action=move-backwards | Makes a robot change its position by moving backwards according to its facing direction. |
| PUT  | /simulations/{sid}/robots/{rid}?action=attack | Makes a robot attack, deleting dinosaurs adjacent to it. |
| GET  | /simulations/{sid} | Receives the state of the specified simulation space. |

**Note**: more descriptive information about the API enpoints can be found by running the application and looking at the swagger documentation.

### Packaging and running as standalone jar

```
lein do clean, ring uberjar
java -jar target/server.jar
```

### Packaging as war

`lein ring uberwar`

