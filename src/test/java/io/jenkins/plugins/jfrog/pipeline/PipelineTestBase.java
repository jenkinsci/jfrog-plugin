package io.jenkins.plugins.jfrog.pipeline;

import hudson.model.Label;
import hudson.model.Slave;
import io.jenkins.plugins.jfrog.jenkins.EnableJenkins;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.fail;

@EnableJenkins
public class PipelineTestBase {

    public JenkinsRule jenkins;
    public Slave slave;
    public static final String SLAVE_LABEL = "TestSlave";

    public void initPipelineTest(JenkinsRule jenkins) {
        this.jenkins = jenkins;
        createSlave(jenkins);
    }

    private void createSlave(JenkinsRule jenkins) {
        if (slave != null) {
            return;
        }
        try {
            slave = jenkins.createOnlineSlave(Label.get(SLAVE_LABEL));
        } catch (Exception e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
