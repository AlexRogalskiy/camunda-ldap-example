# Camunda Enterprise Azure Container Instance deployment

This project shows
- the usage of the Camunda LDAP adapter (org.camunda.bpm.identity:camunda-identity-ldap)
for the synchronization of users and roles/groups from a user directory
- the programmatic association of authorization on Camunda resources to these entities
- testing of those aspects

Note that Camunda disables authorizations by default. They need to be enabled as show in 
the unit test [EngineWithLDAPTest.java](./src/test/java/org/camunda/example/EngineWithLDAPTest.java) and the [application.yaml](./src/main/resources/application.yaml).

The LDAP adapter has been added to the [pom.xml](./pom.xml) and configured in the [test configuration](./src/test/resources/camunda.cfg.xml).

[ApacheDS](https://directory.apache.org/apacheds/) is used as directory sever. The organizational
information fetched from it via LDAP is configured in [ldapConfig.ldif](./src/test/resources/ldapConfig.ldif)

References
- https://docs.camunda.org/manual/latest/user-guide/process-engine/identity-service/  
- https://directory.apache.org/apacheds/  
- https://en.wikipedia.org/wiki/LDAP_Data_Interchange_Format
- https://docs.camunda.org/manual/latest/user-guide/process-engine/authorization-service/  
- https://docs.camunda.org/javadoc/camunda-bpm-platform/7.15/org/camunda/bpm/engine/AuthorizationService.html
- https://docs.camunda.org/manual/latest/webapps/admin/authorization-management/#grant-permission-to-start-processes-from-tasklist



