Relevant links:
- https://central.sonatype.org/pages/apache-maven.html
- https://central.sonatype.org/pages/working-with-pgp-signatures.html
- https://central.sonatype.org/pages/releasing-the-deployment.html

To deploy for the first time, run the following

```shell
export GPG_TTY=$(tty)
gpg --gen-key
gpg2 --list-keys
gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys XXXXXXXX # key is from --list-keys

mvn versions:set -DnewVersion=1.0.2
```
(You need not run the commands that start with `gpg` if you already generated a key before. You would need to run `export GPG...` and `mvn version...` though)

Make the following changes to the pom.xml

```shell
winglam2@asedl:~/iDFlakies$ git diff | cat
diff --git a/pom.xml b/pom.xml
index 175093b..f828eb9 100644
--- a/pom.xml
+++ b/pom.xml
@@ -6,7 +6,7 @@
<groupId>edu.illinois.cs</groupId>
<artifactId>idflakies</artifactId>
-    <version>1.0.2-SNAPSHOT</version>
+    <version>1.0.2</version>
@@ -150,7 +150,11 @@
			<artifactId>maven-gpg-plugin</artifactId>
			<version>1.6</version>
			<executions>
                            <execution>
+                             <configuration>
+                               <executable>gpg2</executable>
+                               <passphrase>XXXXXXXX</passphrase> <!-- passphrase is set to public key outputted from gpg2 list-key -->
+                             </configuration>
			      <id>sign-artifacts</id>
			      <phase>deploy</phase>
			<goals>
```


Modify the `~/.m2/settings.xml` on your machine

```shell
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
	  https://maven.apache.org/xsd/settings-1.0.0.xsd">
	  <localRepository/>
	  <interactiveMode/>
	  <offline/>
	  <pluginGroups>
		<pluginGroup>org.openclover</pluginGroup>
	  </pluginGroups>
	  <mirrors/>
	  <proxies/>
	  <profiles/>
	  <activeProfiles/>
	  <servers>
		<server>
			<id>ossrh</id>
			<username>winglam</username>
			<password>XXXXXXX</password> <!-- password is from account on https://oss.sonatype.org -->
		</server>
	</servers>
</settings>
```
Run `mvn clean deploy -P release` to deploy and then wait about 1 day and the new version should be available on [Maven central](https://mvnrepository.com/artifact/edu.illinois.cs/idflakies). Running `gpgconf --kill gpg-agent` between attempts to deploy may help if gpg was interrupted.

Last deploy was done on asedl in `~/iDFlakies-deploy`.

An example of the log from deploying is in `deploy.log`.
