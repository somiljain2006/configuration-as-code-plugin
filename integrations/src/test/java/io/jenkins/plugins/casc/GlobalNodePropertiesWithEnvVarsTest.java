package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.tools.ToolLocationNodeProperty;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import java.util.Map;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class GlobalNodePropertiesWithEnvVarsTest {

    private final JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()).around(j);

    @Test
    @ConfiguredWithCode("GlobalNodePropertiesWithEnvVarsTest.yml")
    @Envs({@Env(name = "VALUE_1", value = "BAR"), @Env(name = "TEST_GIT_HOME", value = "git-home")})
    public void configureWithEnvVarsTest() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = j.jenkins.getGlobalNodeProperties();
        Map<String, String> envVars =
                nodeProperties.get(EnvironmentVariablesNodeProperty.class).getEnvVars();

        assertThat(envVars.size(), is(2));
        assertThat(envVars.get("FOO"), is("BAR"));
        assertThat(envVars.get("FOO2"), is(""));

        ToolLocationNodeProperty toolLocations = nodeProperties.get(ToolLocationNodeProperty.class);
        assertThat(toolLocations.getLocations(), hasSize(1));
        assertThat(toolLocations.getLocations().get(0).getHome(), is("git-home"));
    }

    @Test
    @ConfiguredWithCode("GlobalNodePropertiesWithEnvVarsTest.yml")
    @Envs({@Env(name = "VALUE_1", value = "BAR"), @Env(name = "TEST_GIT_HOME", value = "git-home")})
    public void export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getJenkinsRoot(context).get("globalNodeProperties");

        String exported = toYamlString(yourAttribute);
        String expected = toStringFromYamlFile(this, "GlobalNodePropertiesWithEnvVarsTestExpected.yml");
        assertThat(exported, Is.is(expected));
    }

    @Test
    @ConfiguredWithCode({"GlobalNodePropertiesWithEnvVarsTest.yml", "GlobalNodePropertiesWithEnvVarsAdditionalTest.yml"
    })
    @Envs({@Env(name = "VALUE_1", value = "BAR"), @Env(name = "TEST_GIT_HOME", value = "git-home")})
    public void mergeEnvVarsFromMultipleConfigFiles() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = j.jenkins.getGlobalNodeProperties();

        EnvironmentVariablesNodeProperty envProperty = nodeProperties.get(EnvironmentVariablesNodeProperty.class);

        assertThat(envProperty, org.hamcrest.Matchers.notNullValue());

        Map<String, String> envVars = envProperty.getEnvVars();

        assertThat(envVars.get("FOO"), is("BAR"));
        assertThat(envVars.get("FOO2"), is(""));
        assertThat(envVars.get("FOO3"), is("baz"));
        assertThat(nodeProperties.size(), is(2));
    }

    @Test
    @ConfiguredWithCode({
        "GlobalNodePropertiesWithEnvVarsTest.yml",
        "GlobalNodePropertiesWithEnvVarsAdditionalTest.yml",
        "GlobalNodePropertiesWithEnvVarsThirdTest.yml"
    })
    @Envs({@Env(name = "VALUE_1", value = "BAR"), @Env(name = "TEST_GIT_HOME", value = "git-home")})
    public void mergeEnvVarsFromThreeConfigFiles() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = j.jenkins.getGlobalNodeProperties();

        EnvironmentVariablesNodeProperty envProperty = nodeProperties.get(EnvironmentVariablesNodeProperty.class);

        assertThat(envProperty, org.hamcrest.Matchers.notNullValue());
        Map<String, String> envVars = envProperty.getEnvVars();

        assertThat(envVars.get("FOO"), is("BAR"));
        assertThat(envVars.get("FOO2"), is(""));
        assertThat(envVars.get("FOO3"), is("baz"));
        assertThat(envVars.get("FOO4"), is("qux"));
    }

    @Test
    @ConfiguredWithCode({"GlobalNodePropertiesWithEnvVarsTest.yml", "GlobalNodePropertiesWithEnvVarsAdditionalTest.yml"
    })
    @Envs({@Env(name = "VALUE_1", value = "BAR"), @Env(name = "TEST_GIT_HOME", value = "git-home")})
    public void unrelatedPropertiesRemainIntactAfterMerge() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = j.jenkins.getGlobalNodeProperties();

        ToolLocationNodeProperty toolLocations = nodeProperties.get(ToolLocationNodeProperty.class);

        assertThat(toolLocations, org.hamcrest.Matchers.notNullValue());
        assertThat(toolLocations.getLocations(), hasSize(1));
        assertThat(toolLocations.getLocations().get(0).getHome(), is("git-home"));
    }

    @Test
    @ConfiguredWithCode({"GlobalNodePropertiesWithEnvVarsTest.yml", "GlobalNodePropertiesWithEnvVarsAdditionalTest.yml"
    })
    @Envs({
        @Env(name = "VALUE_1", value = "BAR"),
        @Env(name = "TEST_GIT_HOME", value = "git-home"),
        @Env(name = "CASC_MERGE_STRATEGY", value = "override")
    })
    public void mergeEnvVarsWithOverrideStrategy() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = j.jenkins.getGlobalNodeProperties();

        EnvironmentVariablesNodeProperty envProperty = nodeProperties.get(EnvironmentVariablesNodeProperty.class);

        assertThat(envProperty, org.hamcrest.Matchers.notNullValue());
        Map<String, String> envVars = envProperty.getEnvVars();

        assertThat(envVars.get("FOO"), is("BAR"));
        assertThat(envVars.get("FOO3"), is("baz"));
    }
}
