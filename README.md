ChallengeVR
===================

Emulates a simple REST service for Property Registration. Uses Scala, Spray and Akka frameworks for a simple actor model based, reactive design approach.

## Design

### REST Layer:

**APIFrontActor**: Create dedicateds instances of APIFrontReplierActors, to process the future responses to every request. 

Translates the REST requests into business layer messages, routing them to the RepoFacadeActor along with a fresh new reference to a APIFrontReplierActor.

**APIFrontReplierActor**: Short life cycle actor, sticked to a single RequestContext object. Its responsability is to wait for the respective response message from business layer (or its own timeout), translate this into a REST response, and take an actor PoisonPill (literally).

### Business Layer

**RepoFacadeActor**: Create and supervise its two child actors (RepoGeoIndexedActor, RepoStorageActor), route to them the received messages properly (3 possibilities: CreateProperty, GetPropertByID or GetPropertiesByRegion).

**RepoStorageActor**: Main storage for properties, maintain the properties indexed by ID. Tell to RepoGeoIndexedActor about new properties, solve the GetById request messages.

**RepoGeoIndexedActor**: Receive the new properties to be geo indexed, solve the GetPropertiesByRegion messages, unsing its internal KDTree data structure.


*Actor Messaging and Dependency*

![](https://raw.githubusercontent.com/darciopacifico/ChallengeVR/master/diagram.png)
