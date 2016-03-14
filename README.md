#Building
fork or clone the project and navigate to it's home directory.  Run `gradle build` in your terminal

#Deploying test server
To deploy the test server run `gradle appengineRun` in your terminal

You can interact with the local api using the Google API explorer: just click "Api Explorer" on the homepage (localhost:8080). Your browser will complain about some security concerns, but you can follow Google's directions on that page to begin making api calls against the local test server.

#Deploying to Google App Engine

Before you deploy to app engine you'll need to do the following:

1. Signup for [Google Cloud](https://appengine.google.com/) if you havent already

2. Create a [Google Cloud project](https://cloud.google.com/appengine/docs/java/console/#create)

3. edit src/main/WEB-INF/appengine-web.xml 's application attribute to match this newly created project id

4. run `gradle appengineUpdate` and follow the instructions. 

Note that sometimes a build can hang while deploying to app engine: https://code.google.com/p/googleappengine/issues/detail?id=9449, use `gradleappengineRollback` to abort a hung deploy, and try again

Once deployed, you will see the landing page, and you can click 'try it out' to begin making calls against your deployment of Sauce Sync.

#Enabling OAuth2 
Follow the instructions [here](https://cloud.google.com/appengine/docs/java/endpoints/auth) under "Creating OAuth 2.0 client IDs" generate the keys you want to use for your clients (iOS, Android, or Web).  

When you've generated the client ids, place them in their respective places inside src/main/java/com/sauce/sync/Constants.java. Unfortunately, they have to be passed via code as Google's annotations require client ids be present at compile time. 

To authenticate in your Android/iOS/Web clients, follow Google's directions for:
 
[Android] (https://cloud.google.com/appengine/docs/java/endpoints/consume_android)

[iOS] (https://cloud.google.com/appengine/docs/java/endpoints/consume_ios)

[Web](https://cloud.google.com/appengine/docs/java/endpoints/consume_js)
 

#Enabling GCM (push notifications)
For your clients to get push notifications, you will have to enable GCM in your Cloud account and place your server key inside src/main/java/com/sauce/sync/Constants.java. 
You should follow Google's GCM documentation [here](https://developers.google.com/cloud-messaging/) and go through the wizard for generating files for your Android or iOS apps.  

When going through Google's wizard (the "Get a Configuration file" buttons) make sure you select the project you created in step #1. 
After going through Google's wizard, you should see your generated GCM server key in your Google Cloud Account in your project under the APIs/credential menu.
 

#Listening for push notifications in clients
Sauce Sync uses topic-style notifications since there is no payload required for clients to stay in sync. A GCM message just tells a client that they should call the 'sync' api method.
Be sure to read Google's documentation on [topic messaging] (https://developers.google.com/cloud-messaging/topic-messaging#sending_topic_messages_from_the_server) if you are unsure of how this works. 

###Android
To listen for GCM topic messages on android, follow [Google's Android documentation] (https://developers.google.com/cloud-messaging/android/client)

Be sure to take a look at [Android's downstream topic messages](https://developers.google.com/cloud-messaging/downstream#receiving-messages-on-an-ios-client-app)

###iOS
To listen for GCM topic messages on iOS, follow [Google's GCM documentation for iOS](https://developers.google.com/cloud-messaging/ios/client)


#Generating SDKs 
The SDKs are generated using Google Cloud Endpoints, so be sure to brush up on [how it works](https://cloud.google.com/appengine/docs/java/endpoints/)

###Android
Follow [Google's Android Cloud Endpoints documentation](https://cloud.google.com/appengine/docs/java/endpoints/consume_android) 

Alternatively, you can find a copy of the generated clients inside the build directory after running `gradle appengineEndpointsExpandClientLibs`

###Javascript
Follow [Google's Cloud Endpoints documentation](https://cloud.google.com/appengine/docs/java/endpoints/consume_js)  The JS client is a lightweight wrapper around the REST api. 

###iOS
Follow [Google's iOS Cloud Endpoints documentation](https://cloud.google.com/appengine/docs/java/endpoints/consume_ios)

#faqs and gotchas
### How do I share data between users? 

You cant (yet).  Sauce Sync is only meant to store per-user data.  The data stored is in your own datastore, however so you can write your own APIs, or extend Sauce Sync.

### Why can't I store nested objects? 

If you try storing an embedded object in the "data" field you will get an illegal argument exception. This is because Google Datastore does not allow you to serialize [lists of maps in embedded entities](https://groups.google.com/forum/#!topic/google-appengine-java/TOsU52hCQlQ). A workaround is to use the @Serialize annotation inside SauceEntity's 'unindexedData' field, but that would only work for unindexed fields.   

### Why is my 64 bit long I synced returning a String?
 
Since Cloud Endpoints sends data over the wire as JSON, all 64 bit integers are turned into strings since [js loses precision after 2^53](https://code.google.com/p/googleappengine/issues/detail?id=9173). Ints, doubles, and floats will work as expected. 

### How come selective sync isnt working? 

Selective sync combines a sync and query operation.  Some selective sync queries will work out of the box using [Google's zigzag merge algorithm](https://cloud.google.com/appengine/articles/indexselection) but others may require you to build a compound index. If you need an index built, Sauce Sync will log an `DatastoreNeedIndexException` with a suggested index. Take a look at /src/main/webapp/WEB-INF/datastore-indexes.xml and add your compound index there then redeploy the app. Note that adding indexes will increase the amount of space your objects consume inside Google's Cloud Datastore.  

 Syncing based on type should work without any developer intervention.  

### How come my query isn't returning data? 

[All queries must utilize an index](https://cloud.google.com/datastore/docs/concepts/queries).  Mark the fields you want querable in the "indexes" array when you sync them.

### Why is Sauce Sync consuming 24 instance hours even when I'm not using it?  

By default, one instance will remain running even when the app is not in use. Since you get 28 instance hours for free, you shouldn't exceed the free quota unless you experience signifigant traffic.   You can change this configuration by tweaking appengine-web.xml.  Take a look at [Google's App engine configuration documentation] (https://cloud.google.com/appengine/docs/java/config/appconfig)


#todo

- create a google cloud deployment (if that's possible) to streamline deployments, otherwise a more sophisticated gradle task could do a better job for deployments.
- make serialization use json, or java serialization in order to allow deeply nested objects

#License
The MIT License (MIT)

Copyright (c) 2015 Sauce

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.