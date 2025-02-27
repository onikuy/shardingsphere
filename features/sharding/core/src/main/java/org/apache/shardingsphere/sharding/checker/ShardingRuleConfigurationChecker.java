/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sharding.checker;

import com.google.common.base.Preconditions;
import org.apache.shardingsphere.infra.config.rule.checker.RuleConfigurationChecker;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingAutoTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.audit.ShardingAuditStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.NoneShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.constant.ShardingOrder;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

/**
 * Sharding rule configuration checker.
 */
public final class ShardingRuleConfigurationChecker implements RuleConfigurationChecker<ShardingRuleConfiguration> {
    
    @Override
    public void check(final String databaseName, final ShardingRuleConfiguration config, final Map<String, DataSource> dataSourceMap, final Collection<ShardingSphereRule> rules) {
        Collection<String> keyGenerators = config.getKeyGenerators().keySet();
        Collection<String> auditors = config.getAuditors().keySet();
        Collection<String> shardingAlgorithms = config.getShardingAlgorithms().keySet();
        checkTableConfiguration(databaseName, config.getTables(), config.getAutoTables(), keyGenerators, auditors, shardingAlgorithms);
        checkKeyGenerateStrategy(databaseName, config.getDefaultKeyGenerateStrategy(), keyGenerators);
        checkAuditStrategy(databaseName, config.getDefaultAuditStrategy(), auditors);
        checkShardingStrategy(databaseName, config.getDefaultDatabaseShardingStrategy(), shardingAlgorithms);
        checkShardingStrategy(databaseName, config.getDefaultTableShardingStrategy(), shardingAlgorithms);
    }
    
    private void checkTableConfiguration(final String databaseName, final Collection<ShardingTableRuleConfiguration> tables, final Collection<ShardingAutoTableRuleConfiguration> autoTables,
                                         final Collection<String> keyGenerators, final Collection<String> auditors, final Collection<String> shardingAlgorithms) {
        for (ShardingTableRuleConfiguration each : tables) {
            checkKeyGenerateStrategy(databaseName, each.getKeyGenerateStrategy(), keyGenerators);
            checkAuditStrategy(databaseName, each.getAuditStrategy(), auditors);
            checkShardingStrategy(databaseName, each.getDatabaseShardingStrategy(), shardingAlgorithms);
            checkShardingStrategy(databaseName, each.getTableShardingStrategy(), shardingAlgorithms);
        }
        for (ShardingAutoTableRuleConfiguration each : autoTables) {
            checkKeyGenerateStrategy(databaseName, each.getKeyGenerateStrategy(), keyGenerators);
            checkAuditStrategy(databaseName, each.getAuditStrategy(), auditors);
            checkShardingStrategy(databaseName, each.getShardingStrategy(), shardingAlgorithms);
        }
    }
    
    private void checkKeyGenerateStrategy(final String databaseName, final KeyGenerateStrategyConfiguration keyGenerateStrategy, final Collection<String> keyGenerators) {
        if (null == keyGenerateStrategy) {
            return;
        }
        Preconditions.checkState(keyGenerators.contains(keyGenerateStrategy.getKeyGeneratorName()),
                "Can not find keyGenerator `%s` in database `%s`.", keyGenerateStrategy.getKeyGeneratorName(), databaseName);
    }
    
    private void checkAuditStrategy(final String databaseName, final ShardingAuditStrategyConfiguration auditStrategy, final Collection<String> auditors) {
        if (null == auditStrategy) {
            return;
        }
        Preconditions.checkState(auditors.containsAll(auditStrategy.getAuditorNames()),
                "Can not find all auditors `%s` in database `%s`.", auditStrategy.getAuditorNames(), databaseName);
    }
    
    private void checkShardingStrategy(final String databaseName, final ShardingStrategyConfiguration shardingStrategy, final Collection<String> shardingAlgorithms) {
        if (null == shardingStrategy || shardingStrategy instanceof NoneShardingStrategyConfiguration) {
            return;
        }
        Preconditions.checkState(shardingAlgorithms.contains(shardingStrategy.getShardingAlgorithmName()),
                "Can not find shardingAlgorithm `%s` in database `%s`.", shardingStrategy.getShardingAlgorithmName(), databaseName);
    }
    
    @Override
    public int getOrder() {
        return ShardingOrder.ORDER;
    }
    
    @Override
    public Class<ShardingRuleConfiguration> getTypeClass() {
        return ShardingRuleConfiguration.class;
    }
}
