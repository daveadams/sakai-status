// StatusServlet.java
//   Reports various Sakai and Tomcat status information
//
// Created 2012-06-13 daveadams@gmail.com
// Last updated 2012-06-13 daveadams@gmail.com
//
// https://github.com/daveadams/sakai-status
//
// This software is public domain. See LICENSE for more information.
//

package org.sakaiproject.status;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.text.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import java.lang.management.ManagementFactory;
import org.apache.commons.dbcp.BasicDataSource;

import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.SakaiProperties;

import org.sakaiproject.memory.api.MemoryService;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;

@SuppressWarnings("unchecked")
public class StatusServlet extends HttpServlet
{
    protected MBeanServer mbs;

    public void init() throws ServletException
    {
        mbs = ManagementFactory.getPlatformMBeanServer();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.setContentType("text/plain");

        try {
            if(request.getPathInfo().equals("/tomcat/mbeans")) {
                reportAllMBeans(response);
            } else if(request.getPathInfo().equals("/tomcat/mbeans/details")) {
                reportAllMBeanDetails(response);
            } else if(request.getPathInfo().equals("/tomcat/mbeans/domains")) {
                reportMBeanDomains(response);
            } else if(request.getPathInfo().equals("/tomcat/current/uris")) {
                reportCurrentURIs(response);
            } else if(request.getPathInfo().equals("/tomcat/threads")) {
                reportThreadPoolStatus(response);
            } else if(request.getPathInfo().equals("/tomcat/threads/details")) {
                reportThreadDetails(response);
            } else if(request.getPathInfo().equals("/tomcat/threads/stacks")) {
                reportThreadStackTraces(response);
            } else if(request.getPathInfo().equals("/tomcat/threadgroups")) {
                reportThreadGroups(response);
            } else if(request.getPathInfo().equals("/tomcat/webapps")) {
                reportWebappStatus(response);
            } else if(request.getPathInfo().equals("/tomcat/webapps/details")) {
                reportDetailedWebappStatus(response);
            } else if(request.getPathInfo().equals("/system/memory")) {
                reportMemoryStatus(response);
            } else if(request.getPathInfo().equals("/system/properties")) {
                reportSystemProperties(response);
            } else if(request.getPathInfo().equals("/sakai/database")) {
                reportSakaiDatabaseStatus(response);
            } else if(request.getPathInfo().equals("/sakai/beans")) {
                reportSakaiBeans(response);
            } else if(request.getPathInfo().equals("/sakai/sessions")) {
                reportActiveSessionCounts(response);
            } else if(request.getPathInfo().equals("/sakai/sessions/counts")) {
                reportAllSessionCounts(response);
            } else if(request.getPathInfo().equals("/sakai/sessions/total")) {
                reportAllSessionTotal(response);
            } else if(request.getPathInfo().equals("/sakai/sessions/users-by-server")) {
                reportUsersByServer(response);
            } else if(request.getPathInfo().equals("/sakai/sessions/all-users")) {
                reportAllUsers(response);
            } else if(request.getPathInfo().equals("/sakai/properties")) {
                reportSakaiProperties(response);
            } else if(request.getPathInfo().equals("/sakai/tools")) {
                reportAllTools(response);
            } else if(request.getPathInfo().startsWith("/sakai/tools/")) {
                reportToolDetails(request.getPathInfo().replace("/sakai/tools/",""), response);
            } else if(request.getPathInfo().equals("/sakai/functions")) {
                reportAllFunctions(response);
            } else if(request.getPathInfo().equals("/sakai/cache")) {
                reportCacheList(response);
            } else if(request.getPathInfo().startsWith("/sakai/cache/")) {
                reportCacheDetails(request.getPathInfo().replace("/sakai/cache/",""), response);
            }
        } catch(Exception e) {
            response.getWriter().print("Exception: " + e.getMessage() + "\n");
        }
    }

    protected Set<ObjectName> findMBeans(String searchString)
    {
        try {
            return mbs.queryNames(new ObjectName(searchString), null);
        } catch(Exception e) {
            return null;
        }
    }


    protected void reportThreadPoolStatus(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();
        for(ObjectName tpName : findMBeans("*:type=ThreadPool,*")) {
            pw.print(mbs.getAttribute(tpName, "name") + ",");
            pw.print(mbs.getAttribute(tpName, "maxThreads") + ",");
            pw.print(mbs.getAttribute(tpName, "currentThreadCount") + ",");
            pw.print(mbs.getAttribute(tpName, "currentThreadsBusy") + "\n");
        }
    }

    protected void reportThreadGroups(HttpServletResponse response) throws Exception
    {
        printThreadGroupDetails(findSystemThreadGroup(), "", response);
    }

    protected void printThreadGroupDetails(ThreadGroup g, String indent, HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();
        ThreadGroup parent = g.getParent();
        String parentName = "";
        if(parent != null) {
            parentName = parent.getName();
        }

        int threadCount = g.activeCount();
        int groupCount = g.activeGroupCount();

        pw.print(indent + g.getName() + "," + parentName + "," +
                 threadCount + "," + groupCount + "\n");

        if(groupCount > 0) {
            ThreadGroup[] children = new ThreadGroup[groupCount];
            g.enumerate(children, false);

            for(ThreadGroup child : children) {
                if(child != null) {
                    printThreadGroupDetails(child, indent + "  ", response);
                }
            }
        }
    }

    protected void reportThreadDetails(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        for(Thread thread : findAllThreads()) {
            if(thread != null) {
                String threadLocation = "";
                try {
                    StackTraceElement ste = thread.getStackTrace()[0];
                    StackTraceElement ste2 = thread.getStackTrace()[1];
                    threadLocation =
                        ste.getClassName() + "." +
                        ste.getMethodName() + "()," +
                        ste.getFileName() + ":" +
                        ste.getLineNumber() + "," +
                        ste2.getClassName() + "." +
                        ste2.getMethodName() + "()," +
                        ste2.getFileName() + ":" +
                        ste2.getLineNumber();
                } catch(Exception e) {
                    threadLocation = "?,?,?,?";
                }
                pw.print(thread.getThreadGroup().getName() + "," +
                         thread.getId() + "," +
                         thread.getName() + "," +
                         thread.getPriority() + "," +
                         thread.getState().name() + "," +
                         (thread.isAlive() ? "" : "notalive") + "," +
                         (thread.isDaemon() ? "daemon" : "") + "," +
                         (thread.isInterrupted() ? "interrupted" : "") + "," +
                         threadLocation + "\n");
            }
        }
    }

    protected ThreadGroup findSystemThreadGroup() throws Exception
    {
        // find the master threadgroup
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = null;
        while((parent = group.getParent()) != null) {
            group = parent;
        }
        return group;
    }

    protected Thread[] findAllThreads() throws Exception
    {
        ThreadGroup group = findSystemThreadGroup();
        Thread[] threads = new Thread[group.activeCount()];
        group.enumerate(threads);

        return threads;
    }

    protected void reportThreadStackTraces(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        for(Thread thread : findAllThreads()) {
            if(thread != null) {
                String stackTrace = "";
                try {
                    StackTraceElement[] stack = thread.getStackTrace();
                    for(StackTraceElement ste : stack) {
                                stackTrace +=
                                    ste.getClassName() + "." +
                                    ste.getMethodName() + "();" +
                                    ste.getFileName() + ":" +
                                    ste.getLineNumber() + " ";
                    }
                } catch(Exception e) {
                    stackTrace += "-";
                }
                pw.print(thread.getThreadGroup().getName() + " " +
                         thread.getId() + " " +
                         stackTrace + "\n");
            }
        }
    }

    protected void reportWebappStatus(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        for(ObjectName appName : findMBeans("*:j2eeType=WebModule,*")) {
            pw.print(mbs.getAttribute(appName, "docBase") + ",");
            pw.print(mbs.getAttribute(appName, "processingTime") + "\n");
        }
    }

    protected void reportDetailedWebappStatus(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        for(ObjectName appName : findMBeans("*:j2eeType=WebModule,*")) {
            for(MBeanAttributeInfo mbai : mbs.getMBeanInfo(appName).getAttributes()) {
                pw.print(mbai.getName() + ",");
                pw.print(mbai.getType() + ",");
                pw.print(mbai.getDescription() + ",");
                pw.print(mbs.getAttribute(appName, mbai.getName()) + "\n");
            }
            pw.print("\n");
            for(MBeanOperationInfo mboi : mbs.getMBeanInfo(appName).getOperations()) {
                pw.print(mboi.getName() + ",");
                pw.print(mboi.getReturnType() + ",");
                pw.print(mboi.getDescription() + "\n");
            }
            pw.print("\n\n");
        }
    }

    protected void reportCurrentURIs(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();
        Object currentUri = null;
        for(ObjectName rpName : findMBeans("*:type=RequestProcessor,*")) {
            currentUri = mbs.getAttribute(rpName, "currentUri");
            if(currentUri != null) {
                pw.print(mbs.getAttribute(rpName, "workerThreadName") + " " + currentUri + "\n");
            }
        }
    }

    protected void reportAllMBeans(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        Set<ObjectInstance> allBeans = mbs.queryMBeans(null, null);
        SortedSet sortedBeanNames = new TreeSet();
        for(ObjectInstance bean : allBeans) {
            sortedBeanNames.add(bean.getObjectName().toString());
        }
        for(Object beanName : sortedBeanNames) {
            pw.print(beanName + "\n");
        }
    }

    protected void reportAllMBeanDetails(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        Set<ObjectInstance> allBeans = mbs.queryMBeans(null, null);
        SortedSet sortedBeanNames = new TreeSet();
        for(ObjectInstance bean : allBeans) {
            sortedBeanNames.add(bean.getObjectName().toString());
        }
        for(Object beanName : sortedBeanNames) {
            pw.print(beanName.toString() + "\n");
            ObjectName beanObjectName = new ObjectName(beanName.toString());
            for(MBeanAttributeInfo mbai : mbs.getMBeanInfo(beanObjectName).getAttributes()) {
                pw.print("  ");
                pw.print(mbai.getName() + ",");
                pw.print(mbai.getType() + ",");
                pw.print(mbai.getDescription() + ",");
                pw.print(mbs.getAttribute(beanObjectName, mbai.getName()) + "\n");
            }
            pw.print("\n");
            for(MBeanOperationInfo mboi : mbs.getMBeanInfo(beanObjectName).getOperations()) {
                pw.print("  ");
                pw.print(mboi.getReturnType() + ",");
                pw.print(mboi.getName() + "(");
                for(MBeanParameterInfo mbpi : mboi.getSignature()) {
                    pw.print(mbpi.getType() + " " + mbpi.getName() + ",");
                }
                pw.print("),");
                pw.print(mboi.getDescription() + "\n");
            }
            pw.print("\n-----------------------------\n\n");
        }
    }

    protected void reportMBeanDomains(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        pw.print("default: " + mbs.getDefaultDomain() + "\n");
        pw.print("domains:\n");
        for(String domain : mbs.getDomains()) {
            pw.print("  - " + domain + "\n");
        }
    }

    protected void reportMemoryStatus(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        pw.print(Runtime.getRuntime().freeMemory() + ",");
        pw.print(Runtime.getRuntime().totalMemory() + ",");
        pw.print(Runtime.getRuntime().maxMemory() + "\n");
    }

    protected void reportSakaiDatabaseStatus(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        BasicDataSource db = (BasicDataSource)ComponentManager.get("javax.sql.DataSource");
        if(db == null) {
            throw new Exception("No data source found.");
        }

        pw.print(db.getNumActive() + "," + db.getNumIdle() + "\n");
    }

    protected void reportSakaiBeans(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        SortedSet sortedBeanNames = new TreeSet();
        for(Object beanName : ComponentManager.getRegisteredInterfaces()) {
            sortedBeanNames.add(beanName);
        }
        for(Object beanName : sortedBeanNames) {
            pw.print(beanName + "\n");
        }
    }

    protected void reportActiveSessionCounts(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        SessionManager sm = (SessionManager)ComponentManager.get("org.sakaiproject.tool.api.SessionManager");
        if(sm == null) {
            throw new Exception("Could not get SessionManager bean.");
        }

        // count sessions in the last hour, half-hour, 15 minutes, five minutes
        pw.print(sm.getActiveUserCount(3600) + ",");
        pw.print(sm.getActiveUserCount(1800) + ",");
        pw.print(sm.getActiveUserCount(900) + ",");
        pw.print(sm.getActiveUserCount(300) + "\n");
    }

    protected void reportAllSessionCounts(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        UsageSessionService uss = (UsageSessionService)ComponentManager.get("org.sakaiproject.event.api.UsageSessionService");
        if(uss == null) {
            throw new Exception("Could not get UsageSessionService bean.");
        }
        Map sessionsByServer = uss.getOpenSessionsByServer();
        int total = 0;
        for(Object key : sessionsByServer.keySet()) {
            Vector serverSessions = (Vector)sessionsByServer.get(key);
            String serverName = ((String)key).replaceAll("-[0-9]+$", "");
            pw.print(serverName + ": " + serverSessions.size() + "\n");
            total += serverSessions.size();
        }
        pw.print("total: " + total + "\n");
    }

    protected void reportAllSessionTotal(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        UsageSessionService uss = (UsageSessionService)ComponentManager.get("org.sakaiproject.event.api.UsageSessionService");
        if(uss == null) {
            throw new Exception("Could not get UsageSessionService bean.");
        }
        Map sessionsByServer = uss.getOpenSessionsByServer();
        int total = 0;
        for(Object key : sessionsByServer.keySet()) {
            Vector serverSessions = (Vector)sessionsByServer.get(key);
            total += serverSessions.size();
        }
        pw.print(total + "\n");
    }

    protected void reportUsersByServer(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        UsageSessionService uss = (UsageSessionService)ComponentManager.get("org.sakaiproject.event.api.UsageSessionService");
        if(uss == null) {
            throw new Exception("Could not get UsageSessionService bean.");
        }
        UserDirectoryService uds = (UserDirectoryService)ComponentManager.get("org.sakaiproject.user.api.UserDirectoryService");
        if(uds == null) {
            throw new Exception("Could not get UserDirectoryService bean.");
        }

        Map sessionsByServer = uss.getOpenSessionsByServer();
        for(Object key : sessionsByServer.keySet()) {
            Vector serverSessions = (Vector)sessionsByServer.get(key);
            String serverName = ((String)key).replaceAll("-[0-9]+$", "");
            pw.print(serverName + ":" + "\n");
            for(Object sessionInfo : serverSessions) {
                String userId = sessionInfo.toString().split(" ")[4];
                String eid = uds.getUser(userId).getDisplayId();
                pw.print("  - " + eid + "\n");
            }
        }
    }

    protected void reportAllUsers(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        UsageSessionService uss = (UsageSessionService)ComponentManager.get("org.sakaiproject.event.api.UsageSessionService");
        if(uss == null) {
            throw new Exception("Could not get UsageSessionService bean.");
        }
        UserDirectoryService uds = (UserDirectoryService)ComponentManager.get("org.sakaiproject.user.api.UserDirectoryService");
        if(uds == null) {
            throw new Exception("Could not get UserDirectoryService bean.");
        }

        Map sessionsByServer = uss.getOpenSessionsByServer();
        for(Object key : sessionsByServer.keySet()) {
            Vector serverSessions = (Vector)sessionsByServer.get(key);
            String serverName = ((String)key).replaceAll("-[0-9]+$", "");
            for(Object sessionInfo : serverSessions) {
                String userId = sessionInfo.toString().split(" ")[4];
                String eid = uds.getUser(userId).getDisplayId();
                pw.print(eid + "\n");
            }
        }
    }

    protected void reportSystemProperties(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        Properties props = System.getProperties();
        Enumeration propNames = props.propertyNames();
        SortedSet sortedPropNames = new TreeSet();
        while(propNames.hasMoreElements()) {
            sortedPropNames.add((String)propNames.nextElement());
        }

        for(Object pName : sortedPropNames) {
            String propertyName = (String)pName;
            String value = props.getProperty(propertyName);
            if(propertyName.startsWith("password")) {
                value = "********";
            }
            pw.print(propertyName + "=" + value + "\n");
        }
    }

    protected void reportSakaiProperties(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        SakaiProperties sp = (SakaiProperties)ComponentManager.get("org.sakaiproject.component.SakaiProperties");
        if(sp == null) {
            throw new Exception("Could not get SakaiProperties bean.");
        }

        Properties props = sp.getRawProperties();
        Enumeration propNames = props.propertyNames();
        SortedSet sortedPropNames = new TreeSet();
        while(propNames.hasMoreElements()) {
            sortedPropNames.add((String)propNames.nextElement());
        }

        for(Object pName : sortedPropNames) {
            String propertyName = (String)pName;
            String value = props.getProperty(propertyName);
            if(propertyName.startsWith("password") || propertyName.endsWith("password")) {
                value = "********";
            }
            pw.print(propertyName + "=" + value + "\n");
        }
    }

    protected void reportAllTools(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        ToolManager tm = (ToolManager)ComponentManager.get("org.sakaiproject.tool.api.ActiveToolManager");
        if(tm == null) {
            throw new Exception("Could not get ToolManager bean.");
        }

        SortedSet<String> sortedToolIds = new TreeSet();
        Set<Tool> toolSet = tm.findTools(null, null);
        for(Tool tool : toolSet) {
            sortedToolIds.add(tool.getId());
        }

        for(String toolId : sortedToolIds) {
            pw.print(toolId + "\n");
        }
    }

    protected void reportToolDetails(String toolId, HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        ToolManager tm = (ToolManager)ComponentManager.get("org.sakaiproject.tool.api.ActiveToolManager");
        if(tm == null) {
            throw new Exception("Could not get ToolManager bean.");
        }

        Tool tool = tm.getTool(toolId);
        if(tool == null) {
            pw.print("ERROR: no such tool ID\n");
            return;
        }

        pw.print("id: " + tool.getId() + "\n");
        pw.print("title: " + tool.getTitle() + "\n");
        pw.print("description: " + tool.getDescription() + "\n");

        Properties regProps = tool.getRegisteredConfig();
        Enumeration propNames = regProps.propertyNames();
        SortedSet sortedPropNames = new TreeSet();
        while(propNames.hasMoreElements()) {
            sortedPropNames.add((String)propNames.nextElement());
        }
        if(sortedPropNames.size() > 0) {
            pw.print("registered_properties:\n");
            for(Object pName : sortedPropNames) {
                String propertyName = (String)pName;
                String value = regProps.getProperty(propertyName);
                pw.print("  " + propertyName + ": " + value + "\n");
            }
        }

        Properties mutableProps = tool.getMutableConfig();
        propNames = mutableProps.propertyNames();
        sortedPropNames = new TreeSet();
        while(propNames.hasMoreElements()) {
            sortedPropNames.add((String)propNames.nextElement());
        }
        if(sortedPropNames.size() > 0) {
            pw.print("mutable_properties:\n");
            for(Object pName : sortedPropNames) {
                String propertyName = (String)pName;
                String value = mutableProps.getProperty(propertyName);
                pw.print("  " + propertyName + ": " + value + "\n");
            }
        }

        Properties finalProps = tool.getFinalConfig();
        propNames = finalProps.propertyNames();
        sortedPropNames = new TreeSet();
        while(propNames.hasMoreElements()) {
            sortedPropNames.add((String)propNames.nextElement());
        }
        if(sortedPropNames.size() > 0) {
            pw.print("final_properties:\n");
            for(Object pName : sortedPropNames) {
                String propertyName = (String)pName;
                String value = finalProps.getProperty(propertyName);
                pw.print("  " + propertyName + ": " + value + "\n");
            }
        }

        Set keywords = tool.getKeywords();
        if(keywords != null) {
            if(keywords.size() > 0) {
                pw.print("keywords:\n");
                for(Object keyword : keywords) {
                    pw.print("  - " + keyword + "\n");
                }
            }
        }

        Set categories = tool.getCategories();
        if(categories != null) {
            if(categories.size() > 0) {
                pw.print("categories:\n");
                for(Object category : categories) {
                    pw.print("  - " + category + "\n");
                }
            }
        }
    }

    protected void reportAllFunctions(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        FunctionManager fm = (FunctionManager)ComponentManager.get("org.sakaiproject.authz.api.FunctionManager");
        if(fm == null) {
            throw new Exception("Could not get FunctionManager bean.");
        }

        SortedSet<String> sortedFunctionNames = new TreeSet();
        List functionList = fm.getRegisteredFunctions();
        for(Object fname : functionList) {
            sortedFunctionNames.add((String)fname);
        }

        for(String functionName : sortedFunctionNames) {
            pw.print(functionName + "\n");
        }
    }

    protected void reportCacheList(HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        CacheManager manager = (CacheManager)ComponentManager.get("org.sakaiproject.memory.api.MemoryService.cacheManager");

        String[] cacheNames = manager.getCacheNames();
        Arrays.sort(cacheNames);
        for (String cacheName : cacheNames) {
            pw.print(cacheName + "\n");
        }
    }

    protected void reportCacheDetails(String cacheName, HttpServletResponse response) throws Exception
    {
        PrintWriter pw = response.getWriter();

        CacheManager manager = (CacheManager)ComponentManager.get("org.sakaiproject.memory.api.MemoryService.cacheManager");

        if(manager == null) {
            throw new Exception("Could not get CacheManager bean.");
        }

        Cache cache = manager.getCache(cacheName);
        if(cache == null) {
            throw new Exception("No such cache name.");
        }

        String evictionPolicy = cache.getMemoryStoreEvictionPolicy().getName();
        CacheConfiguration config = cache.getCacheConfiguration();
        long maxObjects = config.getMaxElementsInMemory();
        long ttl = config.getTimeToLiveSeconds();
        long tti = config.getTimeToIdleSeconds();
        boolean eternal = config.isEternal();
        boolean overflowToDisk = config.isOverflowToDisk();

        net.sf.ehcache.Statistics stats = cache.getStatistics();
        long objectCount = stats.getObjectCount();
        long hits = stats.getCacheHits();
        long misses = stats.getCacheMisses();
        long evictions = stats.getEvictionCount();
        float latency = stats.getAverageGetTime();
        long total = hits + misses;
        long hitRatio = ((total > 0) ? ((100l * hits) / total) : 0);

        pw.print("name: " + cache.getName() + "\n");
        pw.print("memory: " + cache.calculateInMemorySize() + "\n");
        pw.print("objects: " + objectCount + "\n");
        pw.print("maxobjects: " + maxObjects + "\n");
        pw.print("time-to-live: " + ttl + "\n");
        pw.print("time-to-idle: " + tti + "\n");
        pw.print("eviction-policy: " + evictionPolicy + "\n");
        pw.print("eternal: " + eternal + "\n");
        pw.print("overflow-to-disk: " + overflowToDisk + "\n");
        pw.print("evictions: " + evictions + "\n");
        pw.print("latency: " + latency + "\n");
        pw.print("hits: " + hits + "\n");
        pw.print("misses: " + misses + "\n");
        pw.print("total: " + total + "\n");
        pw.print("hitratio: " + hitRatio + "%\n");
    }
}
