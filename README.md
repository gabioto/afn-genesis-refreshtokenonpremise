# Project Title

TODO: One Paragraph of project description goes here

## Getting Started

TODO: These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

* [Java 11 with OpenJDK](https://openjdk.java.net/) - Programming Language.
* [Maven](https://maven.apache.org/) - Dependency Management.

### Installing

Para la compilación, empaquetamiento e instalación del artefacto se ejecutan los siguientes comandos.
para el gestor dependencias Maven.
```
mvn clean
mvn package
```

## Running

Para la ejecución de la función ejecutamos el siguiente comando.
para el gestor dependencias Maven.
```
mvn azure-functions:run
```

Para invocar el endpoint de pruebas se puede utilizar el comando curl.

```
curl http://localhost:7071/api/HttpExample
curl http://localhost:7071/api/HttpExample?name=HTTP%20Query
```

## Running the tests

Para la ejecución de los test ejecutamos el siguiente comando.
para el gestor dependencias Maven.
```
mvn test
```

## Para evaluación de codigo fuente localmente
* checkstyle 
* pmd
* spotbugs 
* jacoco

### Break down into end to end tests

TODO: Listar los test.

### And coding style tests

Para el evaluar el código se utilizan las siguiente herramientas.

* [CheckStyle](https://checkstyle.sourceforge.io/) - Para los estilos de código.
* [Spotbugs](https://spotbugs.github.io/) - Para el análisis estático de código.
* [PMD](https://pmd.github.io/) - Para el análisis estático de código.

Para invocar el análisis de código estático.

Maven:
```
mvn compile test site
```

## Deployment

TODO: Add additional notes about how to deploy this on a live system

## Versioning

We use [SemVer](http://semver.org/) for versioning.

## Authors

* **everis SAC** - *Initial work* - [everis](https://www.everis.com/peru)
