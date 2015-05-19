package org.examhub.service;

import org.examhub.UnitTestConfiguration;
import org.examhub.domain.UserAccount;
import org.examhub.repository.UserAccountRepository;
import org.examhub.utils.AspectUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hieu Do
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UnitTestConfiguration.class)
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        WithSecurityContextTestExecutionListener.class
    })
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(AspectUtils.unwrapProxy(userService), "userAccountRepository", userAccountRepository);
    }

    @Test
    public void getAllUsers_ShouldThrowExceptionWhenUserIsNotAuthenticated() throws Exception {
        thrown.expect(AuthenticationCredentialsNotFoundException.class);
        userService.getAllUsers();
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllUsers_ShouldThrowExceptionWhenUserDoesNotHaveAdminRole() throws Exception {
        thrown.expect(AccessDeniedException.class);
        userService.getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getAllUsers_ShouldReturnAListOfTwoUserAccountsWhenUserHasAdminRole() throws Exception {
        UserAccount userAccount1 = new UserAccount();
        userAccount1.setId(1L);
        userAccount1.setUsername("user1");

        UserAccount userAccount2 = new UserAccount();
        userAccount2.setId(2L);
        userAccount2.setUsername("user2");

        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(userAccount1, userAccount2));

        List<UserAccount> userAccounts = userService.getAllUsers();

        assertThat(userAccounts, hasSize(2));
        assertThat(userAccounts, contains(
            allOf(
                hasProperty("id", is(1L)),
                hasProperty("username", is("user1"))
            ),
            allOf(
                hasProperty("id", is(2L)),
                hasProperty("username", is("user2"))
            )
        ));
    }
}