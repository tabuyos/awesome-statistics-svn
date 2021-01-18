# awesome-statistics-svn
Count the number of lines of code submitted each time.

# Usage
Usage:
    java [-D **`options`** ] -jar awesome-statistics-svn-xxx.jar

eg:

```shell
$ java -Dusername=YourUserName \
  -Dpassword=YourPassWord \
  -Dtemp.dir=/home/$USER/statistic \
  -Ddepth=2 \
  -Dcommit.user=CommitUserName \
  -Dsvn.target.dir=YourSVNPath \
  -jar \
  awesome-statistics-svn-0.1.0.jar
```

Available '**options**':

​    **`command`**:

​        raw command by user, default null, if you execute this option, then you will execute this command directly.

​    **`username`**:

​        username for user of subversion, require

​    **`password`**:

​        password for user of subversion, require

​    **`commit.user`**:

​        search special user of commit, default username.

​    **`svn.target.dir`**:

​        subversion directory, default current directory.

​    **`depth`**:

​        specify the depth for the search, default 0.

​    **`temp.dir`**:

​        temporary log file directory, default $current/temp

​    **`read.log`**:

​        read from the previous log file, default false

​    **`debug`**:

​        turn on debug mode, default false.

​    **`timeout`**:

​		maximum execution time, default 5 minutes.

