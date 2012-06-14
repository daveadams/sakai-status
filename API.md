# sakai-status API #

*Last Updated 2012-06-13*

The *sakai-status* API provides a trivially simple, read-only REST interface
for retrieving metrics and properties from a running Sakai instance. The API
is divided into three logical groupings: Sakai information, JVM information,
and Tomcat information.

All path references below are relative to the `/sakai-status/` base URI. For
example, a reference to `/system/memory` actually means that the full URL would
be something like `http://localhost:8080/sakai-status/system/memory`.

Path requests which are not recognized at the present time return an empty
response with a status code of 200. This is not REST-compliant, and may be
changed in a future release.

## Sakai Information ##

`/sakai/beans`: Lists all Spring beans available in the system by bean ID. eg:

    $ curl http://localhost:8080/sakai-status/sakai/beans
    AssessmentFacadeQueries
    AssessmentGradingFacadeQueries
    ....
    org.sakaiproject.authz.api.AuthzGroupService
    org.sakaiproject.authz.api.FunctionManager
    org.sakaiproject.authz.api.GroupProvider
    ....
    org.sakaiproject.site.api.SiteService
    org.sakaiproject.site.impl.SiteServiceSqlDefault
    ....

`/sakai/cache`: Lists all current caches by name, eg:

    $ curl http://localhost:8080/sakai-status/sakai/cache
    memory.org.sakaiproject.user.api.UserDirectoryService.callCache
    org.hibernate.cache.StandardQueryCache
    org.hibernate.cache.UpdateTimestampsCache
    org.sakaiproject.alias.api.AliasService.callCache
    ....

`/sakai/cache/<cache-name>`: Lists property settings for individual cache
specified, eg:

    $ curl http.../sakai/cache/org.sakaiproject.authz.api.SecurityService.cache
    name: org.sakaiproject.authz.api.SecurityService.cache
    memory: 0
    objects: 79047
    maxobjects: 100000
    time-to-live: 600
    time-to-idle: 600
    eviction-policy: LRU
    eternal: false
    overflow-to-disk: false
    evictions: 310740
    latency: 0.0012587117
    hits: 32565043
    misses: 2794867
    total: 35359910
    hitratio: 92%

`/sakai/database`: Lists current active and idle database connections, eg:

    $ curl http://localhost:8080/sakai-status/sakai/database
    2,18

`/sakai/functions`: Lists all registered functions, eg:

    $ curl http://localhost:8080/sakai-status/sakai/functions
    alias.add
    alias.del
    alias.upd
    annc.all.groups
    annc.delete.any
    ....

`/sakai/properties`: Lists all sakai properties known to Server Configuration
Service and their effective values. Attempts to mask password fields (though
other sensitive fields, eg BasicLTI shared secrets, likely will not be masked).
Note that Spring properties (formatted as `field@bean-id`) do *not* report the
current value of that property on that bean, only what the Server Configuration
Service read in from the `sakai.properties` file. eg:

    $ curl http://localhost:8080/sakai-status/sakai/properties
    accessPath=/access
    activeInactiveUser=true
    archive.toolproperties.excludefilter=password|secret
    assignment.letterGradeOptions=A,A-,B+,B,B-,C+,C,C-,D+,D,D-,F
    auto.ddl=false

`/sakai/sessions`:

`/sakai/sessions/counts`:

`/sakai/sessions/total`:

`/sakai/sessions/users-by-server`:

`/sakai/sessions/all-users`:

`/sakai/tools`: Lists all tool registrations known to Sakai, eg:

    $ curl https://localhost:8080/sakai-status/sakai/tools
    osp.assign
    osp.audience
    osp.evaluation
    ....
    sakai.siteinfo
    sakai.sites
    sakai.sitesetup
    ....

`/sakai/tools/<tool-registration>`: Lists details about the registration of
a particular tool provided in the URL, formatted in a probably-not-entirely-
compliant-YAML format, eg:

    $ curl https://localhost:8080/sakai-status/sakai/tools/sakai.schedule
    id: sakai.schedule
    title: Calendar
    description: For posting and viewing deadlines, events, etc.
    registered_properties:
      calendar: 
      functions.require: calendar.read
      groupAware: true
    mutable_properties:
      calendar: 
      functions.require: calendar.read
      groupAware: true
    categories:
      - course
      - project
      - myworkspace
      - portfolio

## JVM Information ##

`/system/memory`: Reports used and available JVM memory in bytes in CSV format
with the fields: free-memory,total-current-memory,max-possible-memory, eg:

    $ curl http://localhost:8080/sakai-status/system/memory
    3822608656,4277534720,4277534720
    
`/system/properties`: Reports JVM properties in a Java-properties-like text
format, ordered by property name, eg:

    $ curl http://localhost:8080/sakai-status/system/properties
    catalina.base=/opt/sakai/tomcat
    catalina.home=/opt/sakai/tomcat
    catalina.useNaming=true
    common.loader=${catalina.base}/lib,${catalina.base}/lib/*.jar,...
    file.encoding=utf-8
    file.encoding.pkg=sun.io
    file.separator=/
    http.agent=Sakai
    ....

## Tomcat Information ##

`/tomcat/current/uris`:

`/tomcat/mbeans`: Reports the names of all MBeans available to Tomcat. This
is likely a very long list (1533 items long in at least one test case). eg:

    $ curl http://localhost:8080/sakai-status/tomcat/mbeans
    Catalina:j2eeType=Filter,name=CAS Authentication Filter,...
    Catalina:j2eeType=Filter,name=CAS HttpServletRequest Wrapper Filter,... 
    Catalina:j2eeType=Filter,name=CAS Validation Filter,...
    Catalina:j2eeType=Filter,name=CacheFilterForWeek,...
    Catalina:j2eeType=Filter,name=GradebookAuthzFilter,...
    Catalina:j2eeType=Filter,name=MyFacesExtensionsFilter,...

`/tomcat/mbeans/details`:

`/tomcat/mbeans/domains`:

`/tomcat/threadgroups`: Reports threadgroups and their thread counts in a
hierarchical CSV format. On each line, the fields are: threadgroup-name,
threadgroup-parent-name, thread-count, child-group-count. eg:

    $ curl http://localhost:8080/sakai-status/tomcat/threadgroups
    system,,90,3
      main,system,87,2
        QuartzScheduler:QuartzScheduler,main,1,0
        default,main,10,0

`/tomcat/threads`:

`/tomcat/threads/details`:

`/tomcat/threads/stacks`:

`/tomcat/webapps`:

`/tomcat/webapps/details`:

