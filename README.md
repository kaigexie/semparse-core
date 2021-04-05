# semparse-core

This is a bug-free version of [SemParse](https://github.com/jgung/verbnet-parser). The original repository seems to be in lack of maintenance.

All credit goes to the authors of the paper [VerbNet Representations: Subevent Semantics for Transfer Verbs](https://www.aclweb.org/anthology/W19-3318.pdf).

### Dependency Requirements

* Java 8+ and Apache Maven
* For development in an IDE such as IntelliJ IDEA, the corresponding Lombok plugin is required.

### Usage

See ```semparse-core/src/main/java/VerbNetParserTest.java```

To use the parser, you will need to download and unzip the [pre-trained models and mapping files](https://drive.google.com/open?id=1qESz4tlviIjsAYzb8qlUg1ps3o37i6l3).

The root directory should look like this:

```
.
├── LICENSE.txt
├── README.md
├── pom.xml
├── semparse
├── src
└── test.txt
```
