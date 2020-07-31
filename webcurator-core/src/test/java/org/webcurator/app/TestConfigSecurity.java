package org.webcurator.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.webcurator.auth.TransitionalPasswordEncoder;
import org.webcurator.auth.WCTAuthenticationFailureHandler;
import org.webcurator.auth.WCTAuthenticationSuccessHandler;
import org.webcurator.auth.dbms.WCTDAOAuthenticationProvider;
import org.webcurator.auth.ldap.WCTAuthoritiesPopulator;

import javax.sql.DataSource;

@TestConfiguration
@EnableWebSecurity
public class TestConfigSecurity extends WebSecurityConfigurerAdapter {
    @Value("${hibernate.default_schema}")
    private String hibernateDefaultSchema;

    @Value("${ldap.enabled}")
    private String ldapEnabled;

    @Value("${ldap.url}")
    private String ldapUrl;

    @Value("${ldap.usr.search.base}")
    private String ldapUsrSearchBase;

    @Value("${ldap.usr.search.filter}")
    private String ldapUsrSearchFilter;

    @Value("${ldap.group.search.base}")
    private String ldapGroupSearchBase;

    @Value("${ldap.group.search.filter}")
    private String ldapGroupSearchFilter;

    @Value("${ldap.contextsource.root}")
    private String ldapContextSourceRoot;

    @Value("${ldap.contextsource.manager.dn}")
    private String ldapContextSourceManagerDn;

    @Value("${ldap.contextsource.manager.password}")
    private String ldapContextSourceManagerPassword;


    @Autowired
    private LdapContextSource ldapContextSource;

    @Autowired
    private TestConfigBasic baseConfig;

    @Autowired
    private DataSource dataSource;

    @Bean
    public AuthenticationSuccessHandler wctAuthenticationSuccessHandler() {
        WCTAuthenticationSuccessHandler wctAuthenticationSuccessHandler = new WCTAuthenticationSuccessHandler("/curator/home.html", false);
        wctAuthenticationSuccessHandler.setAuthDAO(baseConfig.userRoleDAO());
        wctAuthenticationSuccessHandler.setAuditor(baseConfig.audit());
        wctAuthenticationSuccessHandler.setLogonDurationDAO(baseConfig.logonDuration());
        return wctAuthenticationSuccessHandler;
    }

    @Bean
    public AuthenticationFailureHandler wctAuthenticationFailureHandler() {
        WCTAuthenticationFailureHandler wctAuthenticationFailureHandler = new WCTAuthenticationFailureHandler("/logon.jsp?failed=true");
        wctAuthenticationFailureHandler.setAuditor(baseConfig.audit());
        wctAuthenticationFailureHandler.setUseForward(true);
        return wctAuthenticationFailureHandler;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider bean = new DaoAuthenticationProvider();
        bean.setUserDetailsService(jdbcDaoImpl());
        bean.setPasswordEncoder(passwordEncoder());

        return bean;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // for now support both bcrypt and legacy SHA-1
        return new TransitionalPasswordEncoder();
    }

    @Bean
    public WCTDAOAuthenticationProvider jdbcDaoImpl() {
        WCTDAOAuthenticationProvider bean = new WCTDAOAuthenticationProvider();
        bean.setDataSource(dataSource);
        bean.setUsersByUsernameQuery(getUsersByUsernameQuery(hibernateDefaultSchema));
        bean.setAuthoritiesByUsernameQuery(getAuthoritiesByUsernameQuery());
        bean.setRolePrefix("ROLE_");

        return bean;
    }

    private String getUsersByUsernameQuery(String schema) {
        return "select usr_username, usr_password, usr_active, usr_force_pwd_change from " + schema +
                ".WCTUSER WHERE usr_username = ?";
    }

    public String getAuthoritiesByUsernameQuery() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SELECT distinct PRV_CODE FROM ");
        stringBuilder.append(hibernateDefaultSchema);
        stringBuilder.append(".WCTUSER, ");
        stringBuilder.append(hibernateDefaultSchema);
        stringBuilder.append(".WCTROLE, ");
        stringBuilder.append(hibernateDefaultSchema);
        stringBuilder.append(".USER_ROLE, ");
        stringBuilder.append(hibernateDefaultSchema);
        stringBuilder.append(".ROLE_PRIVILEGE ");
        stringBuilder.append("WHERE ");
        stringBuilder.append("PRV_ROLE_OID = ROL_OID and ");
        stringBuilder.append("URO_USR_OID = USR_OID and ");
        stringBuilder.append("URO_ROL_OID = ROL_OID and ");
        stringBuilder.append("usr_username = ?");

        return stringBuilder.toString();
    }

    @Bean
    public WCTAuthoritiesPopulator authoritiesPopulator() {
        WCTAuthoritiesPopulator bean = new WCTAuthoritiesPopulator();
        bean.setAuthDAO(baseConfig.userRoleDAO());
        return bean;
    }
}
