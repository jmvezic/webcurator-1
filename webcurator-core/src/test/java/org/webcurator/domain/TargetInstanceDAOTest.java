package org.webcurator.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.webcurator.app.TestApplication;
import org.webcurator.domain.model.auth.Agency;
import org.webcurator.domain.model.core.TargetInstance;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestApplication.class})
@SpringBootTest
public class TargetInstanceDAOTest {
    @Autowired
    private TargetInstanceDAO targetInstanceDAO;

    @Autowired
    private UserRoleDAO userRoleDAO;

    @Test
    public void saveAgency() {
        List agencies = userRoleDAO.getAgencies();
        assert agencies.size() > 0;

        Agency agency = (Agency) agencies.get(0);
        assert agency != null;

        agency.setAddress("Auckland");
        userRoleDAO.saveOrUpdate(agency);
    }


    @Test
    public void saveTargetInstance() {
        TargetInstance ti = new TargetInstance();
        ti.setOid(5001L);

        TargetInstance tiExpected = targetInstanceDAO.load(ti.getOid());
        assert tiExpected != null;

        tiExpected.setState(TargetInstance.STATE_ABORTED);
        targetInstanceDAO.save(tiExpected);
    }
}
