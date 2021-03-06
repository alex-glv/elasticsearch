package org.apache.mesos.elasticsearch.systemtest;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

/**
 * Tests REST node discovery
 */
public class DiscoverySystemTest {

    public static final Logger LOGGER = Logger.getLogger(DiscoverySystemTest.class);

    @Test
    public void testNodeDiscoveryRest() throws InterruptedException {
        String slave1Ip = getSlaveIp("mesoses_slave1_1");
        String slave2Ip = getSlaveIp("mesoses_slave2_1");
        String slave3Ip = getSlaveIp("mesoses_slave3_1");

        ElasticsearchNodesResponse nodesResponse = new ElasticsearchNodesResponse(slave1Ip, slave2Ip, slave3Ip);

        assertNodesDiscovered(nodesResponse);
    }

    private void assertNodesDiscovered(ElasticsearchNodesResponse nodesResponse) {
        await().atMost(5, TimeUnit.MINUTES).pollInterval(1, TimeUnit.SECONDS).until(nodesResponse, is(true));
    }

    private String getSlaveIp(String slaveName) {
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withVersion("1.16")
                .withUri("unix:///var/run/docker.sock")
                .build();

        DockerClient docker = DockerClientBuilder.getInstance(config).build();

        InspectContainerResponse response = docker.inspectContainerCmd(slaveName).exec();

        return response.getNetworkSettings().getIpAddress();
    }

    private static class ElasticsearchNodesResponse implements Callable<Boolean> {

        private String ipAddress1;
        private String ipAddress2;
        private String ipAddress3;

        public ElasticsearchNodesResponse(String ipAddress1, String ipAddress2, String ipAddress3) {
            this.ipAddress1 = ipAddress1;
            this.ipAddress2 = ipAddress2;
            this.ipAddress3 = ipAddress3;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                double nodesSlave1 = Unirest.get("http://" + ipAddress1 + ":9200/_nodes").asJson().getBody().getObject().getJSONObject("nodes").length();
                double nodesSlave2 = Unirest.get("http://" + ipAddress2 + ":9200/_nodes").asJson().getBody().getObject().getJSONObject("nodes").length();
                double nodesSlave3 = Unirest.get("http://" + ipAddress3 + ":9200/_nodes").asJson().getBody().getObject().getJSONObject("nodes").length();
                if (!(nodesSlave1 == 3 && nodesSlave2 == 3 && nodesSlave3 == 3)) {
                    return false;
                }
            } catch (UnirestException e) {
                LOGGER.info("Elasticsearch does not yet listen on port 9200");
                return false;
            }
            return true;
        }
    }

}
