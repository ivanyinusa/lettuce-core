package com.lambdaworks.redis.sentinel;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.lambdaworks.Delay.delay;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;

@Ignore("For manual runs only. Fails too often due to slow sentinel sync")
public class SentinelFailoverTest extends AbstractSentinelTest {

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), 26380, MASTER_WITH_SLAVE_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();

        sentinelRule.needMasterWithSlave(MASTER_WITH_SLAVE_ID, port(5), port(6));

    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {

        RedisCommands<String, String> connect = sentinelClient.connect();
        assertThat(connect.ping()).isEqualToIgnoringCase("PONG");

        connect.close();
    }

    @Test
    public void failover() throws Exception {

        RedisClient redisClient = new RedisClient(RedisURI.Builder.redis(TestSettings.host(), port(5)).build());

        RedisCommands<String, String> aHost = redisClient.connect();

        String tcpPort1 = connectUsingSentinelAndGetPort();

        sentinelRule.waitForSlave(MASTER_WITH_SLAVE_ID);
        sentinel.failover(MASTER_WITH_SLAVE_ID).get();

        delay(seconds(5));

        sentinelRule.waitForSlave(MASTER_WITH_SLAVE_ID);

        String tcpPort2 = connectUsingSentinelAndGetPort();
        assertThat(tcpPort1).isNotEqualTo(tcpPort2);
        redisClient.shutdown();
    }

    protected String connectUsingSentinelAndGetPort() {
        RedisCommands<String, String> connectAfterFailover = sentinelClient.connect();
        String tcpPort2 = getTcpPort(connectAfterFailover);
        connectAfterFailover.close();
        return tcpPort2;
    }

    protected String getTcpPort(RedisCommands<String, String> commands) {
        Pattern pattern = Pattern.compile(".*tcp_port\\:(\\d+).*", Pattern.DOTALL);

        Matcher matcher = pattern.matcher(commands.info("server"));
        if (matcher.lookingAt()) {
            return matcher.group(1);
        }
        return null;
    }

}