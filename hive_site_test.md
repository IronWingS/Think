```shell
#!/bin/bash
. hive_config.properties

OLD_NAME=$old_name
NEW_NAME=$new_name

OLD_PASSWORD=$old_password
NEW_PASSWORD=$new_password

OLD_URL=$ori_url
NEW_URL=$new_url

OLD_DRINAME=$old_driname
NEW_DRINAME=$new_driname

HIVE_SITE_DIR=$hive_site_dir

echo $HIVE_SITE_DIR
echo $OLD_NAME
echo $NEW_NAME

echo '$HIVE_SITE_DIR    $OLD_NAME    `$NEW_NAME`'

#sed -i '/<name>javax.jdo.option.ConnectionUserName<\/name>*/{n;s/<value>`$OLD_NAME`<\/value>/<value>$NEW_NAME<\/value>/g}' $HIVE_SITE_DIR

sed "s/$OLD_NAME/$NEW_NAME/g" $HIVE_SITE_DIR
```



```shell
# 元数据库的用户名
old_name=root
new_name=abc

#元数据库的密码
old_password=123456
new_password=123

#元数据库的url
old_url=jdbc:mysql://192.168.1.68:3306/hive
new_url=jdbc:mysql://192.168.1.68:3306/test

#元数据库dirver_name
old_driname=com.mysql.jdbc.Driver
new_driname=com.mysql.derby.Driver

#hive-site.xml文件所在路径
hive_site_dir=/homed/hive_site_test/hive-site.xml
```



