`saved-posts`
===

You are limited to 1000 saved posts on Reddit; as a result,
if you want to save more posts, you need to prune them
every now and then. It's not like you'll ever look at your
saved again posts anyways, but I mean you might as well
have the comfort of having them saved on your desktop,
where you are not limited by a random number, but by disk
space.

I have conveniently decided to extract images and videos
posted to popular sites like imgur, gfycat and even
v.reddit.it to their own files. Comments and text posts are
saved to a text file in JSON format for easy parsing. As
an added bonus, I also threw in async file saving as well.

Building
===

``` shell
git clone https://github.com/AgentTroll/saved-posts.git
cd saved-posts
mvn install
```

Use
===

``` shell
mkdir temp
cd temp
cp target/saved-posts-1.0-SNAPSHOT.jar temp
cd temp
java -jar saved-posts-1.0-SNAPSHOT.jar
```

If you do not have an existing config, one will be created.

You will need to enter your Reddit username, password, and
OAuth2 credentials which can be found by creating a new
app [here](https://www.reddit.com/prefs/apps). The name
and redirect-uri are the only required fields, and set the
app type to script.

Credits
===

Built with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

Uses [OkHttp](https://square.github.io/okhttp/) and
[GSON](https://github.com/google/gson).

Notes
===

Some passwords may have trouble being HTTP POSTed probably
because of some weird encoding thing that is done by either
OkHttp or the Reddit webserver (some passwords flat out
don't even work for signing into the actual website itself)