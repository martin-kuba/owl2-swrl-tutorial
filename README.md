# owl2-swrl-tutorial
Sources for my OWL2 and SWRL tutorial at [http://dior.ics.muni.cz/~makub/owl/#java](http://dior.ics.muni.cz/~makub/owl/#java)

This is a [Maven](https://maven.apache.org/) project, so all needed libraries are automatically downloaded from public repositories.

Compile and run the project from command line:
```
mvn compile exec:exec
```
or open the project in your favourite IDE and run the class cz.makub.Tutorial

The project is old, it has been updated for JDK 17 in the year 2021 by adding `--add-opens java.base/java.lang=ALL-UNNAMED` to compilation and execution.
