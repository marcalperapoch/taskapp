# Task Application

This repository contains the backend offering a Task API that clients can use to create, read, update and delete tasks.

## Table of Contents


* [Project requirements](#requirements)
* [Running standalone Java application](#running_standalone)
* [Running containerized application](#running_container)
* [Publish Docker image](#publish_docker)
* [New to this repo?](#new_to_repo)
* [Architecture Decision Records (ADR)](#adrs)
* [Reflections and leftovers](#reflections)
* [References](#refs)

<a name="requirements"></a>
## Project requirements

* Java 17
* Docker (optional)

<a name="running_standalone"></a>
## Running standalone Java application

The following commands will let you generate a Java jar file with all its dependencies and run it using your local Java environment.

1. Generate the fat jar:
```java
./gradlew shadow
```

2. Run it
```
java -jar build/libs/tasks-app-1.0-all.jar server src/config/tasks-app-config.yaml
```
<a name="running_container"></a>
## Running containerized application

The following commands will let you run the Java application in its own Docker container.

1. Dockerize the application
```
./gradlew docker
```

2. Run the dockerized app
```
./gradlew dockerRun
```

**Notice**: if you change something either in the `Dockerfile` or in the docker gradle plugin, you will need to dockerize your application again. 
Before doing so, stop and remove your container if needed.

1. Stop the dockerized app
```
./gradlew dockerStop
```
2. Remove container
```
./gradlew dockerRemoveContainer
```

<a name="publish_docker"></a>
## Publish Docker image

To be able to deploy our Docker image to Oracle Cloud Infrastructure, image needs to be published to DockerHub (docker.io) so that OCI can read it from there. 
To do so, you can run the following command:

```
./gradlew publishDockerImage
```

That will publish and tag your image as `tasks-app:{version}`. Where `version` is the one defined in the `build.gradle.kts` file.

## API Documentation

### GET /tasks

Returns a list of all the tasks in the system. User of this endpoint can optionally include `limit` and `offset` query parameters to make use of pagination.

```
curl "http://143.47.33.106:8080/tasks?offset=0&limit=10"
```

### GET /tasks/{taskId}

Returns the task identified by `taskId` (`long`) or HTTP `404 Not Found` if the task does not exist.

```
curl "http://143.47.33.106:8080/tasks/2"
```

### POST /tasks

Creates a new task. Returns the new created task or an error if invalid parameters.

```
curl "http://143.47.33.106:8080/tasks/" \
--header 'Content-Type: application/json' \
--data '{
    "description": "My awesome task",
    "endsAtMs": 1688765934
}'
```

### PUT /tasks/{taskId}

Updates an existing task identified by `taskId` (`long`). Returns HTTP `404 Not Found` if the task does not exist or `400 Bad Request` if invalid parameters.

```
curl --location --request PUT "http://143.47.33.106:8080/tasks/1" \
--header 'Content-Type: application/json' \
--data '{
    "description": "My First Task (modified)",
    "endsAtMs": 1688765934,
    "completed": true
}'
```

### DELETE /tasks/{taskId}

Removes an existing task identified by `taskId` (`long`). Returns `204 No Content` if the task has been successfully deleted or is no longer present in the system.

```
curl --request DELETE "http://143.47.33.106:8080/tasks/1"
```


<a name="new_to_repo"></a>
## New to this Repo?

If you just want to start digging into source code, a good place to start is:
```java
com.perapoch.tasksapp.resources.TaskResource
```

<a name="adrs"></a>
## Architecture Decision Records (ADR)

### ADR1. Persistence storage
This backend application uses "**key-value**" stores to persist data in the database. The key is either a numeric or string value, and the value/payload is the **JSON**
representation of the Java object being stored. 
Additionally, it also has support for a unique index.

Motivations:
- the model is simple enough to be able to create a taskId -> task association. No need for strong relationships such foreign keys
- accessing data by primary key is extremely fast and using the right transaction isolation level, locks can be acquired per row, which means that modifying a single record don't 
affect performance of other row reads/modifications.
- using JSON as payload offers backwards and forward compatibility. New fields can be ignored by older versions and older fields can also be ignored by newer versions of the application. 
- single way of persisting data. If you know how to use a HashMap you know how to persist data.
- even though there's only support for a secondary index, it shouldn't be hard to extend to support N of them. The only reason of just having one now is that it's all that was 
needed to fulfill current requirements. However, must be noticed that a String index can provide a lot of flexibility if used as multiple other fields concatenation. Another aspect 
of keeping that N low, is that more index also means more performance penalty at insertion time.

About the chosen database vendor. **H2** has been the one that seemed more appropriated for this task, since being an embedded database it's way easier to set up and deploy than other
relational databases such **MySQL or Postgres**. However, it's important to notice that changing to any of those other databases that use SQL language would be a trivial change on the
application side, which means that almost all the code could be reused if at some point it was decided to change. 

### ADR2. Caching layer
This application does heavy usage of caching under the assumption that, for the type of application, there will usually be more reads than writes.

Concretely, it uses **local** server caching in an LRU fashion, which has several implications:

* Only clients that read from the same server they just wrote are guaranteed to see their own writes.
  * For more information about this see the next section.
* Clients can eventually get stale data. However, at some point after cache expiration they should start getting fresh data.
* **Distributed caching** such Memcached or Redis was considered out of the scope for this task, but it would be a must if this was to be deployed into multiple servers under heavy 
traffic. If that was the case, a valid solution would be to use the local cache as first layer and delegate to the distributed one or even to the database in the case of miss. 
* With the right **load balancing** strategy, client requests could be routed to the same server, so that local cache becomes more effective.

### ADR3. Read your own writes
This api has been designed to provide strong consistency on client requests addressing specific `{taskId}`, and eventual consistency on requests targeting multiple tasks.
For instance, when a client updates a task by doing `PUT /tasks/{taskId}` it will immediately get the most recent version of this `task` in both, that same endpoint's response 
and the corresponding `GET /tasks/{taskID}` assuming both calls go to the same server. However, calling `GET /tasks` may return stale data until the cache is refreshed.
Therefore, using sticky sessions at load-balancer level, could improve the client experience.

### ADR4. Id generation
An important aspect of the persistence solution is the generation of arbitrary new ids.
A typical approach to this is to rely on the **auto-increment** functionality from the database. However, this face scalability bottlenecks when it's time to escalate your 
databases between multiple databases/shards. Proposed solution relies on its own table (which would not be sharded) and caching. Everytime it's out of ids it fetch a range from 
this table, so that it "reserves" a bunch of ids to be used by that server only.    


### ADR5. Gradle vs Maven
This project used Gradle because:
* I'm more familiar with it
* It's less verbose than Maven
* Gradle has a really nice documentation and supports both Groovy and Kotlin scripting.

### ADR6. Dependency injection
Even Dropwizard already comes with **H2K**, an out-of-the-box dependency injection mechanism, for this project I decided to use the **Google Guice** plugin for Dropwizard.
Motivation is that Guice is a more popular choice and provides a clean way to structure the project into different modules, each of them
grouping classes sharing a same purpose. Moreover, if at some point it needs to be migrated to any other framework,
it's very likely that we can reuse all the Guice setup instead of having the need of learning a new way for wiring and injecting dependencies.

### ADR7. Testing
This project heavy relies on integration testing to make sure the all the requested functionalities work as expected.
**Integration tests** can be found under `src/integraionTest` while regular **unit tests** live under `src/test`. Apart from making a clear separation of the type of test,
this structure would also allow to use different gradle configurations for them. For instance, we could choose to run integration tests only in our CD/CI pipeline but not locally 
by default, since integration tests usually take more time to run.

### ADR8. Thread-safety and concurrency
This API has been designed with the goal of maximizing the number of requests/second it can handle without affecting application correctness. Because of that, it uses caching and 
eventual consistency as explained before, but also immutable classes when possible and database **row locking and transactions**.
Thread-safeness is checked via integration tests but with more time it would have also been nice to put the application to its limits by doing load tests, for instance using the 
Gatling library. 

### ADR7. Observability

Main stack:
* Logback is used **logging** since it already comes by out-of-the-box with Dropwizard.
* Custom **health check** `DatabaseHealthCheck` is used  to verify that the application can talk to the database.
  * `http://{host}:8081/healthcheck`
* **Dropwizard metrics**: right now we are only using the default ones that come with Dropwizard plus the ones for timing the "/tasks" endpoint. Further actions will include 
things such: queries/second and query times. Ideally, we could display them in *Grafana* and create dashboards and alerts.

> Metrics are a key element for incident detection, and if this was about something we would launch and have real usage, it would be a requirement to have some place to visualize 
them rather than just in the `http://{host}:8081/metrics`. The only reason I didn't do it yet, is because I'm already out of time :D.


<a name="reflections"></a>
## Reflections and leftovers

Doing this challenge has been a very **fun learning experience**. However, I must admit that it took me way longer than expected, here are some reasons:
* I had zero experience with Dropwizard (I have used Spring Boot tho), so I spent a lot of time reading through the docs.
* I had zero experience with Oracle Cloud Infrastructure, so learning how to deploy a Docker image there has also been fun.
* After almost 6 years of pure RPC, thinking about REST again has been nice.
* I don't like using solutions that I don't fully understand how they work. For instance when I looked at the [Hibernate](https://www.dropwizard.io/en/latest/manual/hibernate.html)
module for Dropwizard I didn't feel ready to debug those magic queries it does for you.
  * Disclaimer: I have nothing against Hibernate, but given the time that I had to make complete this challenge I didn't feel it was something I could 
  learn quickly with enough guarantees to come up with some good solution.
* Implementing my own persistence framework. At the beginning I was about to persist everything in memory, using pure Java data structures. However, since it was not clear that
this was an option, I decided to go with something in the middle, like using H2.
* Invested a lot of time in producing good quality code vs tooling or setting up infrastructure (as described later).

That said, there have been many things I didn't have time to complete despite the amount of invested hours. Here are the main ones:
- Api versioning: I think it could be nice to version endpoints in a way similar to `/v1/tasks` to facilitate breaking change adoption by the clients.
- Automate deployments: feel like I have done half of the path, which is deplying the Docker image to DockerHub, but when it comes to Oracle Cloud, I have done all the provisioning
and container management manually. I suspect (but don't know) that this could be automated, maybe even using Terraform or similar, which would make deployments a bit more pleasant.
- Custom metrics: as said before, I consider metrics a fundamental thing to have when deploying something to production. Without metrics and logs we are blind, and we really need 
them to detect possible bugs or even to be able to react/prevent incidents. Using only the default ones feels better than nothing, but I would really liked spending more time on 
this to add some that cover the database, at least.
- Security checks: right now nothing prevents an attacker to send millions of request to my service, probably causing what is called a DDOS attack. While this arguably could be 
solved on a layer on top of my container (i.e: the load balancer), adding some rate limiter would be nice.
- Jetty performance tuning: right now the app is using the defaults provided by Dropwizard, which despite being reasonable, would have been nice to spend more time understanding
them and how to tweak them if needed.
- Load testing: another nice to have would have been setting up Gatling and perform load tests to see where are the application bottlenecks.
- Maybe it would have been nice to consider also a "PATCH" endpoint to allow partial update of the task.
- A bit more unit testing although a lot of efforts have been put to make sure integration tests cover most of the cases.

<a name="refs"></a>
## References

* [Dropwizard](https://www.dropwizard.io/en/latest/index.html)
* [Gradle Shadow](https://github.com/johnrengelman/shadow)
* [Gradle Docker](https://github.com/palantir/gradle-docker)
* [Dropwizard Guice Plugin](https://github.com/xvik/dropwizard-guicey)
* [H2 Database](https://www.h2database.com/html/main.html)
* [Gatling](https://gatling.io/)


