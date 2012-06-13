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

`/sakai/beans`:

`/sakai/cache`:

`/sakai/cache/<cache-name>`:

`/sakai/database`:

`/sakai/functions`:

`/sakai/properties`:

`/sakai/sessions`:

`/sakai/sessions/counts`:

`/sakai/sessions/total`:

`/sakai/sessions/users-by-server`:

`/sakai/sessions/all-users`:

`/sakai/tools`:

`/sakai/tools/<tool-registration>`:

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

