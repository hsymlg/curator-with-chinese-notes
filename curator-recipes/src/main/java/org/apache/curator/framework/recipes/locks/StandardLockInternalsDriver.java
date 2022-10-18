/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.curator.framework.recipes.locks;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class StandardLockInternalsDriver implements LockInternalsDriver
{
    static private final Logger log = LoggerFactory.getLogger(StandardLockInternalsDriver.class);

    @Override
    public PredicateResults getsTheLock(CuratorFramework client, List<String> children, String sequenceNodeName, int maxLeases) throws Exception
    {
        //获取当前节点在所有子节点排序后的索引位置
        int             ourIndex = children.indexOf(sequenceNodeName);
        //判断当前节点是否处于子节点中
        validateOurIndex(sequenceNodeName, ourIndex);
        //InterProcessMutex的构造方法,会将maxLeases初始化为1
        //ourIndex必须为0,才能使得getsTheLock为true,也就是说,当前节点必须是basePath下的最小节点,才能代表获取到了锁
        boolean         getsTheLock = ourIndex < maxLeases;
        //如果获取不到锁,则返回上一个节点的名称,用作对其设置监听
        String          pathToWatch = getsTheLock ? null : children.get(ourIndex - maxLeases);

        return new PredicateResults(pathToWatch, getsTheLock);
    }

    @Override
    public String createsTheLock(CuratorFramework client, String path, byte[] lockNodeBytes) throws Exception
    {
        String ourPath;
        if ( lockNodeBytes != null )
        {
            ourPath = client.create().creatingParentContainersIfNeeded().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path, lockNodeBytes);
        }
        else
        {
            ourPath = client.create().creatingParentContainersIfNeeded().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
        }
        return ourPath;
    }


    @Override
    public String fixForSorting(String str, String lockName)
    {
        return standardFixForSorting(str, lockName);
    }

    public static String standardFixForSorting(String str, String lockName)
    {
        int index = str.lastIndexOf(lockName);
        if ( index >= 0 )
        {
            index += lockName.length();
            return index <= str.length() ? str.substring(index) : "";
        }
        return str;
    }

    static void validateOurIndex(String sequenceNodeName, int ourIndex) throws KeeperException
    {
        if ( ourIndex < 0 )
        {
            //可能会由于连接丢失导致临时节点被删除,因此这里属于保险措施
            throw new KeeperException.NoNodeException("Sequential path not found: " + sequenceNodeName);
        }
    }
}
