package org.camunda.example;

import lombok.extern.slf4j.Slf4j;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.camunda.bpm.engine.AuthorizationException;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.List;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.junit.Assert.assertEquals;

@Slf4j
@RunWith(FrameworkRunner.class)
@CreateDS(name = "testDS", partitions = {@CreatePartition(name = "test", suffix = "dc=camunda,dc=org")})
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP", address = "localhost", port = 3433)})
@ApplyLdifFiles({"ldapConfig.ldif"})
@Deployment(resources = {"process.bpmn"})
public class EngineWithLDAPTest extends AbstractLdapTestUnit {

    public static final String GROUP_ID = "ThePixies";
    public static final String USERID_IN_GROUP = "bfrancis";
    public static final String PD_KEY = "MyProcess";
    public static final String USER_NO_GROUP = "bbuilder";
    @ClassRule
    public static ProcessEngineRule rule = new ProcessEngineRule();

    @BeforeClass
    public static void setUp() {
        rule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

        /* grant the group READ and CREATE_INSTANCE permission for the process
         so members can see the process definition and create instances */
        Authorization pdAuth = authorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        pdAuth.setGroupId(GROUP_ID);
        // pdAuth.setUserId(USERID_IN_GROUP);
        pdAuth.setResource(Resources.PROCESS_DEFINITION);
        //the resource id for a process definition is the process definition key
        pdAuth.setResourceId(PD_KEY);
        pdAuth.addPermission(Permissions.CREATE_INSTANCE);
        pdAuth.addPermission(Permissions.READ);
        pdAuth.addPermission(Permissions.READ_HISTORY);
        authorizationService().saveAuthorization(pdAuth);

        Authorization createPIAuth = authorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        createPIAuth.setGroupId(GROUP_ID);
        // createPIAuth.setUserId(USERID_IN_GROUP);
        createPIAuth.setResource(Resources.PROCESS_INSTANCE);
        //the resource id for a process definition is the process definition key
        createPIAuth.setResourceId("*");
        createPIAuth.addPermission(Permissions.CREATE);
        authorizationService().saveAuthorization(createPIAuth);
    }

    @Test
    public void shouldReturnLDAPUsers() {
        var users = identityService().createUserQuery().list();
        log.info("Users found:");
        for (var user : users) {
            log.info("User Id: {}, First name: {} Last name:{}", user.getId(), user.getFirstName(), user.getLastName());
        }
        var user = identityService().createUserQuery().userId(USERID_IN_GROUP).singleResult();
        assertEquals(user.getLastName(), "Francis");
    }

    @Test
    public void shouldBelongToCorrectGroups() {
        var groups = identityService().createGroupQuery().list();
        log.info("Groups found:");
        for (var group : groups) log.info("GroupId: {}", group.getId());

        var group = identityService().createGroupQuery().groupId(GROUP_ID).singleResult();
        assertEquals(group.getId(), GROUP_ID);

        var users = identityService().createUserQuery().memberOfGroup(group.getId()).list();
        org.assertj.core.api.Assertions.assertThat(users).extracting("id").containsExactly(USERID_IN_GROUP);
    }

    @Test
    public void shouldBeAllowedToStartMyProcess() {
//        identityService().setAuthenticatedUserId(USERID_IN_GROUP);
//        authorizationService().isUserAuthorized(USERID_IN_GROUP, List.of(GROUP_ID), Permissions.CREATE_INSTANCE,PD_KEY)
        var auths = authorizationService().createAuthorizationQuery().list();
        for (var auth : auths) log.info(auth.toString());

        identityService().setAuthentication(USERID_IN_GROUP, List.of(GROUP_ID),List.of("default"));
        ProcessInstance pi = runtimeService().startProcessInstanceByKey(PD_KEY);
        assertThat(pi).isStarted();
    }

    @Test(expected = AuthorizationException.class)
    public void shouldNotBeAllowedToStartMyProcess() {
        identityService().setAuthenticatedUserId(USER_NO_GROUP);
        ProcessInstance pi = runtimeService().startProcessInstanceByKey(PD_KEY);
    }
}
