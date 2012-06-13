# sakai-status #

last updated: 2012-06-13  
author: David Adams (daveadams@gmail.com)  
github url: https://github.com/daveadams/sakai-status

*sakai-status* is a servlet that can be built and installed into a Sakai
CLE instance which provides a simple, unauthenticated, read-only REST
interface for gathering metrics and examining settings from a running
instance of Sakai.

## Compiling ##

To build *sakai-status* against your instance of Sakai, simply clone this
directory into a subdirectory of your main Sakai source code, edit the
`<parent>` section of the `pom.xml` file to match your pathing and version
numbering, and then build with the usual method, usually something like:

    $ mvn install

Including `sakai-status` as a module in your base `pom.xml` file will ensure
it is built automatically when a full build is done.

## Deploying ##

You can deploy the *sakai-status* war file using the typical method from the
`sakai-status` root directory:

    $ mvn sakai:deploy

Or if you prefer more control, the build process generates a war file in the
`target` directory and installs it to your local Maven repository, which you
can copy into your Tomcat `webapps` directory manually.

## Usage ##

The *sakai-status* servlet provides a trivially simple read-only REST interface
for querying various metrics and settings of the currently running Sakai
instance. This interface will be available under the `/sakai-status/` URI stem
on whatever Connector you've deployed the webapp. For example, in the most
common case, using default Tomcat settings, and deploying the *sakai-status*
war file to the Tomcat `webapps` directory, the REST interface will be
available to the local server via `http://localhost:8080/sakai-status/...`.

_NOTE_: In the simple configuration described above, the `/sakai-status/` URI
stem will be openly available, without authentication, to anyone who can reach
port 8080 on your server, including through Apache proxies and load balancers.
While care has been taken to obscure some common sensitive values, the
information available through *sakai-status* is not appropriate for public
viewing, and care should be taken to restrict access to that URI stem to
appropriate networks or administrators. More information can be found in the
*Security* section below.

In the simplest use case, an administrator may query *sakai-status* by using
`curl` from the command line, like so:

    $ curl http://localhost:8080/sakai-status/sakai/database
    1,9

In this case, the response indicates that Sakai currently has nine idle
database connections and one currently in use. Full details of the REST
interface and the meanings of its responses are available in the `API.md`
file.

## Security ##

As mentioned above, *sakai-status* provides much information that is highly
sensitive, both from system security and user privacy standpoints, and as
an intentional part of its design, it does not enforce any security on
incoming requests. Thus, it is critical for the Sakai administrator to take
appropriate steps to ensure that the servlet is inaccessible to the public.

If deployed in the default Tomcat `webapps` directory, the safest
architecture is to place Tomcat behind an Apache httpd reverse proxy or a
load-balancer and use the capabilities of those platforms to restrict access
to the `/sakai-status/` URI stem to appropriate networks and/or authorized
users.

Another alternative that goes a step further is to isolate the *sakai-status*
servlet in a separate Tomcat `Service`. This is relatively simple to set up
using code such as the following within the Tomcat `server.xml` configuration
file:

    <Service name="Internal">
      <Connector port="9880" URIEncoding="UTF-8" />
      <Engine name="Internal" defaultHost="localhost">
        <Host name="localhost" appBase="internal-webapps" />
      </Engine>
    </Service>

In this example, you would deploy the *sakai-status* war file to the
`internal-webapps` directory under Tomcat rather than the default `webapps`.

The *sakai-status* interface would then be available on a different port
and could be more easily isolated from user-oriented Sakai webapps.

## Change History ##

See `CHANGELOG.md`

## Future Plans ##

See `ROADMAP.md`

## License ##

This work is dedicated to the public domain. No rights are reserved. See
`LICENSE` for more information.

