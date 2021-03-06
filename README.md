# LaPasse for Java

CQRS and Redux inspired library for Java.

LaPasse requires at minimum Java 7.

[![Build Status](https://travis-ci.org/cookingfox/lapasse-java.svg?branch=master)](https://travis-ci.org/cookingfox/lapasse-java)
[![codecov](https://codecov.io/gh/cookingfox/lapasse-java/branch/master/graph/badge.svg)](https://codecov.io/gh/cookingfox/lapasse-java)
[![Dependency Status](https://www.versioneye.com/user/projects/57c7f55d69d9490042f73165/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/57c7f55d69d9490042f73165)

### _Note: proper documentation is in the works!_

## Download

[![Download](https://api.bintray.com/packages/cookingfox/maven/lapasse-java/images/download.svg)](https://bintray.com/cookingfox/maven/lapasse-java/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.cookingfox/lapasse/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.cookingfox/lapasse)

The distribution is hosted on [Bintray](https://bintray.com/cookingfox/maven/lapasse-java/view).
To include the package in your projects, you can add the jCenter repository.

### Gradle

Add jCenter to your `repositories` block (not necessary for Android - jCenter is the default
repository):

```groovy
repositories {
    jcenter()
}
```

and add the project to the `dependencies` block in your `build.gradle`:

```groovy
dependencies {
    compile 'com.cookingfox:lapasse:0.5.6'
}
```

### Maven

Add jCenter to your repositories in `pom.xml` or `settings.xml`:

```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <url>http://jcenter.bintray.com</url>
    </repository>
</repositories>
```

and add the project to the `dependencies` block in your `pom.xml`:

```xml
<dependency>
    <groupId>com.cookingfox</groupId>
    <artifactId>lapasse</artifactId>
    <version>0.5.6</version>
</dependency>
```

### Rx extension

There is also an [RxJava](https://github.com/ReactiveX/RxJava) extension for the library. To
download it, replace `lapasse` with `lapasse-rx` in the above dependency declarations.

## Usage

Javadocs (hosted on javadoc.io):
- [LaPasse (core)](http://www.javadoc.io/doc/com.cookingfox/lapasse/0.5.6)
- [LaPasse Rx extension](http://www.javadoc.io/doc/com.cookingfox/lapasse-rx/0.5.6)
- [LaPasse Compiler (annotation processor)](http://www.javadoc.io/doc/com.cookingfox/lapasse-compiler/0.5.6)

## Samples

You can find the following sample applications in the [`lapasse-samples`](lapasse-samples) folder:
- [Tasks app "vanilla"](lapasse-samples/src/main/java/com/cookingfox/lapasse/samples/tasks_vanilla):
uses the core LaPasse library (no extensions) and no annotations.
- [Tasks app with annotations](lapasse-samples/src/main/java/com/cookingfox/lapasse/samples/tasks_annotations):
uses the core LaPasse library (no extensions) and annotations.
- [Tasks app with Immutables](lapasse-samples/src/main/java/com/cookingfox/lapasse/samples/tasks_immutables):
uses the core LaPasse library (no extensions), annotations and
[Immutables library](http://immutables.github.io/).
- [Counter example with Rx](lapasse-samples/src/main/java/com/cookingfox/lapasse/samples/counter_rx):
uses the LaPasse library with the Rx extension and no annotations.
