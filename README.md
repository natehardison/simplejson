Simple JSON REST Service
Nate Hardison <natehardison@gmail.com>
15 February 2013

INSTALLATION
------------
This project was developed using Java 1.6.0_37 on OSX 10.8.2 using Maven
3.0.3, Apache Tomcat 6, and MySQL 5.6.10.

Hibernate is configured to use a MySQL database named `simplejson` owned by
user `root` with no password. You can change these settings in
`src/main/resources/hibernate.cfg.xml` if desired. Set up this database
before proceeding, and ensure your MySQL server is running.

The `pom.xml` file in this directory is set up so that you can easily use
Maven to compile and run the service:

`mvn tomcat:run`

The first time, you'll have to wait for the Tomcat Maven plugin and the
Hibernate, MySQL connector, and Jersey JARs to download.

API
---
GET /car                  => Retrieve all cars
GET /owner                => Retrieve all owners
GET /person               => Retrieve all people
GET /truck                => Retrieve all trucks

GET /car/{vin}            => Retrieve car by VIN
GET /owner/{id}           => Retrieve owner by ID
GET /person/{id}          => Retrieve person by ID
GET /truck/{vin}          => Retrieve truck by VIN

GET /owner/{id}/vehicles  => Retrieve all vehicles for owner with ID

POST /car                 => Create car
POST /owner               => Create owner
POST /person              => Create person
POST /truck               => Create truck

POST /car/{vin}           => Update car by VIN
POST /owner/{id}          => Update owner by ID
POST /person/{id}         => Update person by ID
POST /truck/{vin}         => Update truck by VIN

POST /owner/{id}/vehicles => Add vehicle to owner with ID

PUT /car/{id}             => Create or update car by ID
PUT /owner/{id}           => Create or update owner by ID
PUT /person/{id}          => Create or update person by ID
PUT /truck/{id}           => Create or update truck by ID
