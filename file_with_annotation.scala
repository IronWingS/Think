package cn.ipanel.homed.repots
import cn.ipanel.common._
import cn.ipanel.homed.repots.BusinessVisitCount.{getHalfHourRangeList}
import cn.ipanel.utils.{DBUtils, DateUtils}
import com.mysql.jdbc.StringUtils
import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, SaveMode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** 直播频道统计
  * 计算指标：频道收视率、收视份额、到达率、观看人数、注册人数、观看时长、点击次数
  *
  * @author lizhy@20181116
  */

object LiveChannelStatisticsNew {
  var partAmt = 128
  var channelTypeMap = new mutable.HashMap[String, String] // @lixfy
  def main(args: Array[String]): Unit = {
    val sparkSession: SparkSession = SparkSession("LiveChannelStatistics")
    val sc: SparkContext = sparkSession.sparkContext
    val hiveContext: HiveContext = sparkSession.sqlContext
    val sqlContext = sparkSession.sqlContext
    sc.getConf.registerKryoClasses(Array(classOf[UserArray], classOf[ChannelCnt], classOf[ChannelLiveByHalfHour], classOf[ChannelSubType]))
    var date = DateUtils.getYesterday()
    //    var partAmt = 200
    if (args.length == 0) {
      System.err.println("请输入正确参数：统计日期,分区数")
    } else if(args.length == 1){
      date = args(0)
    } else {
      date = args(0)
      partAmt = args(1).toInt
    }
    //    println("直播频道用户保存开始：" + DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    //    saveUserArray(date, hiveContext, partAmt)
    //    println("直播频道用户保存结束：" + DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))

    channelCategoryProcess(sparkSession, hiveContext) // @lixfy 获得频道类型的HashMap数据
    sqlContext.udf.register("judge_channel_category", judgeChannelCategory _)// @lixfy 注册udf

    println("直播频道统计开始：" + DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    liveChannelStatistics(date, hiveContext, sparkSession, partAmt)
    println("直播频道统计结束")
    sparkSession.stop()
  }

  def liveChannelStatistics(date: String, hiveContext: HiveContext, sparkSession: SparkSession, partAmt: Int) = {
    hiveContext.sql("use bigdata")
    //    val regionDF = getRegionDf(sparkSession, hiveContext)
    val regionDF = cn.ipanel.utils.RegionUtils.getRegions(hiveContext)
      .selectExpr("region_id as f_area_id","region_name as f_region_name","city_id as f_city_id","city_name as f_city_name","province_id as f_province_id","province_name as f_province_name")
    println("半小时：" + DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    //    busVisitCountByHalfhour(sparkSession.sqlContext,date,"HALFHOUR",regionDF) //按每半小时统计访问人数，用于直播半小曲线图
    channelLineCountByHalfHour(sparkSession, regionDF, date, partAmt) //按每半小时统计
    // channelProgramByHalfHour(date, hiveContext) //按半小时带节目的
    if(AnalysisProperties.ANALYSIS_TIMETYPE_DAY == "yes"){
      println("按天统计开始:"+DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
      channelLineCountByType("DAY", hiveContext, sparkSession, regionDF, date, partAmt) //按天
    }else{
      println("analysis.timetype.day!=yes 不执行按天统计")
    }
    if(AnalysisProperties.ANALYSIS_TIMETYPE_WEEK == "yes"){
      println("按周统计开始:"+DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
      channelLineCountByType("WEEK", hiveContext, sparkSession, regionDF, date, partAmt) //按周
    }else{
      println("analysis.timetype.week!=yes 不执行周统计")
    }
    if(AnalysisProperties.ANALYSIS_TIMETYPE_MONTH == "yes"){
      println("按月统计开始:"+DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
      channelLineCountByType("MONTH", hiveContext, sparkSession, regionDF, date, partAmt) //按月
    }else{
      println("analysis.timetype.month!=yes 不执行月统计")
    }
    if(AnalysisProperties.ANALYSIS_TIMETYPE_YEAR == "yes"){
      println("按年统计开始:"+DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
      channelLineCountByType("YEAR", hiveContext, sparkSession, regionDF, date, partAmt)
    }else{
      println("analysis.timetype.year!=yes 不执行年统计")
    }
    if(AnalysisProperties.ANALYSIS_TIMETYPE_7DAYS == "yes"){
      println("按最近七天统计开始:"+DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
      channelLineCountByType("7DAYS", hiveContext, sparkSession, regionDF, date, partAmt) //按7天内
    }else{
      println("analysis.timetype.7days!=yes 不执行最近七天统计")
    }
    /*println("季度："+ DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    channelLineCountByType("QUARTER",hiveContext,sparkSession,regionDF,date,partAmt) //按季度
    println("年："+ DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    channelLineCountByType("YEAR",hiveContext,sparkSe;ion,regionDF,date,partAmt) //按年*/
    /*println("30天内：" + DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    channelLineCountByType("30DAYS", hiveContext, sparkSession, regionDF, date, partAmt) *///按30天内
    /*println("1年内："+ DateUtils.getNowDate(DateUtils.YYYY_MM_DD_HHMMSS))
    channelLineCountByType("1YEAR",hiveContext,sparkSession,regionDF,date,partAmt) //按1年内*/
  }


  /**
    * 半小时统计
    * @param sqlContext
    * @param date
    * @param regionDf
    */
  def busVisitCountByHalfhour(sqlContext: HiveContext, date: String,  countType:String, regionDf: DataFrame) = {
    import sqlContext.implicits._
    val daySql =
      s"""
         |select devicetype as terminal,regionid,playtype,starttime,endtime,userid
         |from ${Tables.ORC_VIDEO_PLAY} t
         |where day = '$date' and userid <> '' and playtype='live' and userid is not null
      """.stripMargin
    //where day = '$date' and playtime > 0 and userid <> '' and userid is not null
    val halfHourDf = sqlContext.sql(daySql)
      .map(x => {
        val busVisitBuffer = new ListBuffer[(String, Int, Int, String, String, String, Long, Long,String)] //date,hour,range,terminal,regionId,terminal,playTime,visitTimes
        val terminal = x.getAs[String]("terminal")
        val regionId = x.getAs[String]("regionid")
        val startTime = x.getAs[String]("starttime")
        var endTime = x.getAs[String]("endtime")
        if (DateUtils.dateStrToDateTime(endTime, DateUtils.YYYY_MM_DD_HHMMSS).toString(DateUtils.YYYYMMDD) > date) {
          endTime = DateUtils.dateStrToDateTime(date, DateUtils.YYYYMMDD).plusDays(1).plusSeconds(-1).toString(DateUtils.YYYY_MM_DD_HHMMSS)
        }
        var serviceType = x.getAs[String]("playtype")
        val userId = x.getAs[String]("userid")
        if (serviceType == GatherType.one_key_timeshift) {
          serviceType = GatherType.TIME_SHIFT_NEW
        }
        val rangeList = getHalfHourRangeList(startTime, endTime) // @lixfy 这里是把startTime 到 endTime中间的时间按半个小时切分为一个list
        for (list <- rangeList) { // 然后还有playTime这个的计算没有看懂
          val hour = list._1
          val range = list._2
          val playTime = list._3
          var visitTimes = 1
          busVisitBuffer += ((date, hour, range, terminal, regionId, serviceType, playTime, visitTimes, userId))
        }
        busVisitBuffer.toIterator
      })
      .flatMap(x => x)

    //时长、播放次数、用户数、活跃用户数
    val countDf = halfHourDf.map(x => {
      //(date, hour, range, terminal, regionId, serviceType, playTime, visitTimes,userId)
      val date = x._1
      val hour = x._2
      val range = x._3
      val terminal = x._4
      val regionId = x._5
      val serviceType = x._6
      val playTime = x._7
      val playCount = x._8
      val userId = x._9
      val key = date + "," + hour + "," + range + "," + terminal + "," + regionId + "," + serviceType + "," + userId
      val value = (playTime,playCount)
      (key,value)
    }).reduceByKey((x,y) => {
      (x._1 + y._1,x._2 + y._2)
    })
      .map(x => {
        val keyArr = x._1.split(",")
        val date = keyArr(0)
        val hour = keyArr(1).toInt
        val range = keyArr(2).toInt
        val terminal = keyArr(3).toInt
        val regionId = keyArr(4)
        val serviceType = keyArr(5)
        //        val userId = keyArr(6)
        val playTime = x._2._1
        val playCount = x._2._2
        var activeuserCount = 0
        if(playCount >= 2){//活跃用户条件，访问次数大于等于两次
          activeuserCount = 1
        }
        val userCount = 1
        val key = date + "," + hour + "," + range + "," + terminal + "," + regionId + "," + serviceType
        val value = (playTime,playCount,activeuserCount,userCount)
        (key,value)
      }).reduceByKey((x,y) =>{
      (x._1 + y._1,x._2 + y._2,x._3 + y._3, x._4 + y._4)
    }).map(x => {
      val keyArr = x._1.split(",")
      val date = keyArr(0)
      val hour = keyArr(1).toInt
      val timeRange = keyArr(2).toInt
      val terminal = keyArr(3).toInt
      val regionId = keyArr(4)
      val serviceType = keyArr(5)
      val value = x._2
      val playTime = value._1
      val playCount = value._2
      val activeUserCount = value._3
      val userCount = value._4
      BusinessVisitByHalfHour(date,hour,timeRange,terminal,regionId,serviceType,playTime,playCount,userCount,activeUserCount)
    }).toDF()
    //    println("接半小时countDf记录数：" + countDf.count())
    val channelRegionDf = countDf.join(regionDf,countDf("f_region_id") === regionDf("f_area_id"))
    val savedf = channelRegionDf.selectExpr("f_date", "f_hour", "f_timerange", "f_province_id", "f_province_name", "f_city_id",
      "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_service_type", "f_play_time", "f_play_count", "f_user_count", "f_active_user_count")
    DBUtils.saveDataFrameToPhoenixNew(savedf, Tables.T_BUS_VISIT_HALFHOUR)
  }


  def channelProgramByHalfHour(date: String, hiveContext: HiveContext) = {
    val share =
      s"""
         |select
         |a.f_region_id,a.f_hour,a.f_timerange,a.f_terminal,a.f_channel_id,
         |sum(a.f_share_count) as f_share_count
         |from
         |(select regionId as f_region_id,hour(reportTime) as f_hour,
         |(case when minute(reportTime)>30 then 60 else 30 end) as f_timerange,
         |cast(deviceType AS INT) as f_terminal,
         |exts['ID'] as f_channel_id,
         |1 as f_share_count
         |from  orc_user_behavior where day=$date and reportType='ShareSuccess') a
         |group by a.f_region_id,a.f_hour,a.f_timerange,a.f_terminal,a.f_channel_id+
      """.stripMargin
    val live =
      s"""
         |select  '$date' as f_date,
         |b.f_hour,b.f_timerange,
         |b.f_province_id,b.f_province_name,b.f_city_id,
         |b.f_city_name,b.f_region_id,b.f_region_name,b.f_terminal,
         |b.f_channel_name,b.f_channel_id,
         |concat_ws(',',collect_set(b.f_channel_type)) as f_channel_type,
         |b.f_channel_id,b.f_channel_name,sum(b.f_play_time) as f_play_time,
         |sum(b.f_play_count) as f_play_count,
         |count(distinct(b.userid)) as f_play_user_amt,
         |concat_ws(',',collect_set(b.f_event_id)) as f_event_id,
         |concat_ws(',',collect_set(b.f_event_name)) as f_event_name
         |from
         |(select
         |a.f_hour,gettimerangebyhalfhour(a.f_timerange) as f_timerange,
         |a.f_play_time,
         |a.f_play_count,
         |a.f_terminal,a.f_region_id,a.f_region_name,a.f_city_id,a.userid,
         |a.f_city_name,a.f_province_id,a.f_province_name,a.f_channel_type,
         |a.f_channel_id,
         |a.f_channel_name,
         |a.f_event_id,a.f_event_name
         |from t_channel_basic a)  b
         |group by
         |b.f_hour,b.f_timerange,
         |b.f_terminal,b.f_region_id,b.f_region_name,b.f_city_id,
         |b.f_city_name,b.f_province_id,b.f_province_name,b.f_channel_name,b.f_channel_id
      """.stripMargin
    hiveContext.sql("use bigdata")
    val shareDF = hiveContext.sql(share)
    hiveContext.udf.register("gettimerangebyhalfhour", gettimerangebyhalfhour _)
    val livedf = hiveContext.sql(live)
    val finaldf = shareDF.join(livedf, Seq("f_terminal", "f_hour", "f_timerange", "f_channel_id", "f_region_id"), "right")
      .selectExpr("f_date", "f_hour", "f_timerange", "f_province_id",
        "f_province_name", "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal",
        "f_channel_type", "f_channel_id", "f_channel_name", "f_play_time", "f_play_count", "f_share_count",
        "f_play_user_amt", "f_event_id", "f_event_name").show()
    //DBUtils.saveDataFrameToPhoenixNew(finaldf, Tables.T_CHANNEL_LIVE_HALFHOUR)
  }

  def gettimerangebyhalfhour(timerange: Int) = {
    var timerangeNew = 0
    if (timerange == 5 || timerange == 10 || timerange == 15 || timerange == 20 || timerange == 25 || timerange == 30) {
      timerangeNew = 30
    }
    else {
      timerangeNew = 60
    }
    timerangeNew
  }


  /** *
    * 按统计类型统计：日、周、月、季、年、7天前、30天前
    *
    * @param sparkSession
    * @param regionDF
    * @param date
    */
  def channelLineCountByType(countType: String, sqlContext: HiveContext, sparkSession: SparkSession, regionDF: DataFrame, date: String, partAmt: Int) = {
    val countFromDate =
      countType match {
        case "DAY" => date
        case "WEEK" => DateUtils.getFirstDateOfWeek(date)
        case "MONTH" => DateUtils.getFirstDateOfMonth(date)
        case "QUARTER" => DateUtils.getFirstDateOfQuarter(date)
        case "YEAR" => DateUtils.getFirstDateOfYear(date)
        case "7DAYS" => DateUtils.getDateByDays(date, 7)
        case "30DAYS" => DateUtils.getDateByDays(date, 30)
        case "1YEAR" => DateUtils.getDateByDays(date, 365)
        case _ => "-1"
      }
    //    val channelLiveDf = getChannelUserCount(countFromDate, date, sqlContext, sparkSession, partAmt: Int)
    val channelLiveDf = getChannelUserCountNew(countType,countFromDate, date, sqlContext, sparkSession, partAmt, regionDF) //改从清洗基础分表取数据
    //F_PROVINCE_ID,F_PROVINCE_NAME,F_CITY_ID,F_CITY_NAME,F_REGION_ID,F_REGION_NAME,F_TERMINAL,F_CHANNEL_TYPE,F_CHANNEL_ID,F_CHANNEL_NAME,f_play_time,F_PLAY_COUNT,F_SHARE_COUNT,F_PLAY_USER_AMT
    if (countType == "7DAYS" || countType == "30DAYS" || countType == "1YEAR") {
      //      DBUtils.excuteSqlPhoenix(s"delete from ${Tables.T_CHANNEL_LIVE_HIS} where f_end_date < '$date' ") //取消删除操作
      val resultDf = channelLiveDf.selectExpr("f_start_date", "f_end_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type", "f_channel_id", "f_channel_name",
        "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt","judge_channel_category(f_channel_id) as f_channel_category")
      DBUtils.saveDataFrameToPhoenixNew(resultDf, Tables.T_CHANNEL_LIVE_HIS)
    } else if (countType == "DAY") {
      //日统计 增加环比前期值 播放次数&播放时长@20191212
      val lastDate = DateUtils.getNDaysBefore(1,countFromDate)
      println("lastDate:" + lastDate)
      val lastDaySql = s"(select f_play_count,f_play_time,f_region_id,f_terminal,f_channel_id from ${Tables.T_CHANNEL_LIVE_DAY} where f_date='$lastDate') as last_day"
      val lastDayDf = DBUtils.loadDataFromPhoenix2(sqlContext,lastDaySql)
        .selectExpr("f_play_count as f_play_count_early","f_play_time as f_play_time_early","f_region_id as l_region_id","f_terminal as l_terminal","f_channel_id as l_channel_id")
      val joinLastDay = channelLiveDf.join(lastDayDf,channelLiveDf("f_region_id")===lastDayDf("l_region_id") && channelLiveDf("f_terminal")===lastDayDf("l_terminal") &&channelLiveDf("f_channel_id")===lastDayDf("l_channel_id") ,"left_outer")
      val resultDf = joinLastDay.selectExpr("f_start_date as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type",
        "f_channel_id", "f_channel_name", "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt",
        "nvl(f_play_count_early,0) as f_play_count_early","nvl(f_play_time_early,0) as f_play_time_early","judge_channel_category(f_channel_id) as f_channel_category")

      /*val resultDf = channelLiveDf.selectExpr("f_start_date as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type",
        "f_channel_id", "f_channel_name", "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt")*/
      DBUtils.saveDataFrameToPhoenixNew(resultDf, Tables.T_CHANNEL_LIVE_DAY)
    } else if (countType == "WEEK") {
      ////周统计 增加环比前期值@20191212
      val lastWeek = DateUtils.getNDaysBefore(7,countFromDate)
      println("lastWeek:" + lastWeek)
      val lastWeekSql = s"(select f_play_count,f_play_time,f_region_id,f_terminal,f_channel_id from ${Tables.T_CHANNEL_LIVE_WEEK} where f_date='$lastWeek') as last_week"
      val lastWeekDf = DBUtils.loadDataFromPhoenix2(sqlContext,lastWeekSql)
        .selectExpr("f_play_count as f_play_count_early","f_play_time as f_play_time_early","f_region_id as l_region_id","f_terminal as l_terminal","f_channel_id as l_channel_id")
      val joinLastWeek = channelLiveDf.join(lastWeekDf,channelLiveDf("f_region_id")===lastWeekDf("l_region_id") && channelLiveDf("f_terminal")===lastWeekDf("l_terminal") &&channelLiveDf("f_channel_id")===lastWeekDf("l_channel_id") ,"left_outer")
      val resultDf = joinLastWeek.selectExpr("f_start_date as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type",
        "f_channel_id", "f_channel_name", "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt",
        "nvl(f_play_count_early,0) as f_play_count_early","nvl(f_play_time_early,0) as f_play_time_early","judge_channel_category(f_channel_id) as f_channel_category")

      /*val resultDf = channelLiveDf.selectExpr("f_start_date as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type",
        "f_channel_id", "f_channel_name", "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt")*/
      DBUtils.saveDataFrameToPhoenixNew(resultDf, Tables.T_CHANNEL_LIVE_WEEK)
    } else if (countType == "MONTH") {
      //月统计 增加同环比前期值@20191212
      val lastMonth = DateUtils.dateStrToDateTime(countFromDate,DateUtils.YYYYMMDD).plusMonths(-1).toString(DateUtils.YYYYMM)
      println("lastMonth:" + lastMonth)
      val lastMonthSql = s"(select f_play_count,f_play_time,f_region_id,f_terminal,f_channel_id from ${Tables.T_CHANNEL_LIVE_MONTH} where f_date='$lastMonth') as last_week"
      val lastMonthDf = DBUtils.loadDataFromPhoenix2(sqlContext,lastMonthSql)
        .selectExpr("f_play_count as f_play_count_early","f_play_time as f_play_time_early","f_region_id as l_region_id","f_terminal as l_terminal","f_channel_id as l_channel_id")
      val joinLastMonth = channelLiveDf.join(lastMonthDf,channelLiveDf("f_region_id")===lastMonthDf("l_region_id") && channelLiveDf("f_terminal")===lastMonthDf("l_terminal") &&channelLiveDf("f_channel_id")===lastMonthDf("l_channel_id") ,"left_outer")
      val resultDf = joinLastMonth.selectExpr("substring(f_start_date,0,6) as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type",
        "f_channel_id", "f_channel_name", "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt",
        "nvl(f_play_count_early,0) as f_play_count_early","nvl(f_play_time_early,0) as f_play_time_early","judge_channel_category(f_channel_id) as f_channel_category")

      /*val resultDf = channelLiveDf.selectExpr("substring(f_start_date,0,6) as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type",
        "f_channel_id", "f_channel_name", "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt")*/
      DBUtils.saveDataFrameToPhoenixNew(resultDf, Tables.T_CHANNEL_LIVE_MONTH)
    } else if (countType == "QUARTER") {
      val resultDf = channelLiveDf.selectExpr("substring(f_start_date,0,6) as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type", "f_channel_id", "f_channel_name",
        "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt","judge_channel_category(f_channel_id) as f_channel_category")
      DBUtils.saveDataFrameToPhoenixNew(resultDf, Tables.T_CHANNEL_LIVE_QUARTER)
    } else if (countType == "YEAR") {
      val resultDf = channelLiveDf.selectExpr("substring(f_start_date,0,4) as f_date", "f_province_id", "f_province_name",
        "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type", "f_channel_id", "f_channel_name",
        "round(f_play_time,4) as f_play_time", "f_play_count", "f_share_count", "f_play_user_amt","judge_channel_category(f_channel_id) as f_channel_category")
      DBUtils.saveDataFrameToPhoenixNew(resultDf, Tables.T_CHANNEL_LIVE_YEAR)
    }
  }

  /**
    * 直播频道统计-按半小时分段统计
    *
    * @param sparkSession
    * @param date 日期yyyymmdd
    * @return
    */
  def channelLineCountByHalfHour(sparkSession: SparkSession, regionDF: DataFrame, date: String, partAmt: Int) = {
    import sparkSession.sqlContext.implicits._
    val hiveContext = sparkSession.sqlContext
    //数据源以切换
    val sqlNew =
      s"""
         |select day as f_date, userId as f_user_id ,cast(deviceId as string) as f_device_id,
         |cast(deviceType as int) as f_terminal,
         |regionId as f_region_id, nvl(cast(serviceId as string),'${GatherType.UNKNOWN_CHANNEL}') as f_channel_id,
         |startTime as f_channel_start_time ,endTime as f_channel_end_time,
         |playTime as f_play_time
         |from orc_video_play where day=$date and playType='${GatherType.LIVE_NEW}'
      """.stripMargin
    // and playtime>0 and serviceId is not null
    //分享量
    /*val share =
      s"""
         |select regionId as f_region_id,hour(reportTime) as f_hour,
         |(case when minute(reportTime)>30 then 60 else 30 end) as f_timerange,
         |cast(deviceType AS INT) as f_terminal,
         |exts['ID'] as f_channel_id,
         |1 as f_share_count
         |from  orc_user_behavior where day=$date and reportType='ShareSuccess'
      """.stripMargin
    hiveContext.sql("use bigdata")*/
    val liveRdd = hiveContext.sql(sqlNew).rdd
    //    val shareDF = hiveContext.sql(share)

    val liveDF = liveRdd//.repartition(partAmt)    //@lixfy 这里没有直接用DataFrame，而是先用了一个rdd。因为rdd的话是不能知道内部表结构的，所以就得先用getAs的这种方法先得到表结构。
      .map(x => {                                  // 那么问题来了，为什么不直接用DataFrame呢，而且sql语句的 `from orc_video_play`也和之前的sql不一致，为什么要搞两套方案呢
      val buffer = new ListBuffer[(ChannelLiveByHalfHour)]
      val date = x.getAs[String]("f_date")
      val userId = x.getAs[String]("f_user_id")
      val deviceId = x.getAs[String]("f_device_id")
      val terminal = x.getAs[Integer]("f_terminal")
      val regionId = x.getAs[String]("f_region_id")
      val channelId = x.getAs[String]("f_channel_id")
      val startTime = x.getAs[String]("f_channel_start_time")
      val playTimes = x.getAs[Long]("f_play_time")
      val listBuffer = process(startTime, playTimes)
      for (list <- listBuffer) {
        val hour = list._1
        val timeRange = list._2
        val startT = list._3
        val endT = list._4
        val plays = list._5
        val playCount = list._6
        val dateTime = DateUtils.transformDateStr(startT.substring(0, 10), DateUtils.YYYY_MM_DD, DateUtils.YYYYMMDD)
        buffer += ChannelLiveByHalfHour(dateTime, hour, timeRange, userId, deviceId, regionId, terminal, channelId, startT, endT, plays, playCount)
        // @lixfy 按照时间段，分割原数据表，创建一个新的表，是不是如果用DataFrame，用SQL写的话，没法表达这个半小时的概念，因为我看process函数里面还是搞得挺复杂的
        // 具体的业务逻辑我也没有完全看完，只是看了个大概，估计就是因为这一部分  <<< -------------------------  这里后面得细致的看一下，为啥用SQL写不出来。
      }
      buffer.toList
    }).flatMap(x => x).toDF() // @lixfy 最终还是得转换为一个DataFrame
    val channelSubTypeDF = getChannelTypeDf(sparkSession, hiveContext)
    val df = liveDF.join(channelSubTypeDF, liveDF("f_channel_id") === channelSubTypeDF("chanel_id"), "left_outer").drop("chanel_id")
    val channelRegionDF = df.join(regionDF, df("f_region_id") === regionDF("f_area_id"), "left_outer").drop("f_area_id")
    //    processDfByHalfHour(sparkSession, channelRegionDF, shareDF)
    processDfByHalfHourNew(sparkSession, channelRegionDF)
  }

  /**
    * 按30分划分时间片段，播放时长
    * 返回[hour,timerange,startTime,endTime,playTimes.playCount]
    *
    * @param startTime
    * @param playTimes
    * @return
    */
  def process(startTime: String, playTimes: Long): ListBuffer[(Integer, Integer, String, String, Long, Integer)] = {
    val list = new ListBuffer[(Integer, Integer, String, String, Long, Integer)]
    var start = DateUtils.transferYYYY_DD_HH_MMHHSSToDate(startTime)
    val end = start.plusSeconds(playTimes.toInt)
    var hour: Integer = 0
    val m = start.getMinuteOfHour
    var timeRange: Integer = 30
    var endTemp = end
    var flag = true
    var plays = 0
    var i = 0
    var playCount = 0
    while (flag) {
      i match {
        case 0 => {
          if (m < 30) {
            endTemp = start.plusMinutes(30 - m).minusSeconds(start.getSecondOfMinute)
          } else {
            endTemp = start.plusMinutes(60 - m).minusSeconds(start.getSecondOfMinute)
            if(end.compareTo(endTemp) < 0){
              endTemp = end
            }
          }
          i = 1
          playCount = 1
        }
        case _ => {
          endTemp = start.plusMinutes(30)
          if(end.compareTo(endTemp) < 0){
            endTemp = end
          }
          playCount = 0
        }
      }
      hour = start.getHourOfDay
      timeRange = if (start.getMinuteOfHour < 30) 30 else 60
      if (end.compareTo(endTemp) > 0) {
        val startStr = start.toString(DateUtils.YYYY_MM_DD_HHMMSS)
        val endStr = endTemp.toString(DateUtils.YYYY_MM_DD_HHMMSS)
        //跨天
        if (endTemp.getDayOfYear != start.getDayOfYear) {
          plays = 86400 - start.getSecondOfDay
        } else {
          plays = endTemp.getSecondOfDay - start.getSecondOfDay
        }
        list += ((hour, timeRange, startStr, endStr, plays.toLong, playCount))
      } else {
        flag = false
        val startStr = start.toString(DateUtils.YYYY_MM_DD_HHMMSS)
        val endStr = end.toString(DateUtils.YYYY_MM_DD_HHMMSS)
        //跨天
        if (endTemp.getDayOfYear != start.getDayOfYear) {
          plays = 86400 - start.getSecondOfDay
        } else {
          plays = end.getSecondOfDay - start.getSecondOfDay
        }
        list += ((hour, timeRange, startStr, endStr, plays.toLong, playCount))
      }
      start = endTemp
    }
    list
  }
  /**
    * 业务处理逻辑
    * 将基础数据按指定条件聚合
    *
    * @param sparkSession
    * @param dataFrame
    */
  def processDfByHalfHourNew(sparkSession: SparkSession, dataFrame: DataFrame) = {
    val hiveContext = sparkSession.sqlContext
    dataFrame.registerTempTable("t_chinel_program_live")

    //按同一用户、同一终端、同一区域、同一时间段，统一频道，聚合播放时长，点击次数
    //加上设备id
    val channel =
    """
      |select f_date,f_hour,f_timerange,count(distinct f_user_id) as f_play_user_amt,
      |f_province_id,f_province_name,
      |f_city_id,f_city_name,f_region_id,f_region_name,f_terminal,
      |concat_ws(',',collect_set(f_channel_type)) as f_channel_type,
      |f_channel_id,f_channel_name,
      |cast(sum(f_play_time)/3600 as double) as f_play_time,
      |sum(f_play_count) as f_play_count
      |from t_chinel_program_live
      |group by f_date,f_hour,f_timerange,f_terminal,f_channel_id,f_channel_name,
      |f_region_id,f_region_name,f_province_id,f_province_name,f_city_id,f_city_name
    """.stripMargin
    val channelDF = hiveContext.sql(channel)

    val channelResultDF = channelDF.selectExpr("f_date", "f_hour", "f_timerange", "f_province_id", "f_province_name",
      "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal", "f_channel_type", "f_channel_id", "f_channel_name",
      "f_play_time", "f_play_count", "0 as f_share_count", "f_play_user_amt","judge_channel_category(f_channel_id) as f_channel_category")
    DBUtils.saveDataFrameToPhoenixNew(channelResultDF, Tables.T_CHANNEL_LIVE_HALFHOUR)
  }

  /**
    * 业务处理逻辑
    * 将基础数据按指定条件聚合
    *
    * @param sparkSession
    * @param dataFrame
    */
  def processDfByHalfHour(sparkSession: SparkSession, dataFrame: DataFrame, shareDF: DataFrame) = {
    val hiveContext = sparkSession.sqlContext
    dataFrame.registerTempTable("t_chinel_program_live")
    shareDF.registerTempTable("t_share")
    //频道分享点击次数
    val shareSql =
      """
        |select f_region_id as f_region_id_share ,f_hour as f_hour_share,f_timerange as f_timerange_share,
        |f_terminal as f_terminal_share,f_channel_id as f_channel_id_share,
        |sum(f_share_count) as f_share_count
        |from t_share
        |group by f_region_id,f_hour,f_timerange,f_terminal,f_channel_id
      """.stripMargin
    val shareCount = hiveContext.sql(shareSql)

    //按同一用户、同一终端、同一区域、同一时间段，统一频道，聚合播放时长，点击次数
    //加上设备id
    val channel =
    """
      |select f_date,f_hour,f_timerange,count(distinct f_user_id) as f_play_user_amt,
      |f_province_id,f_province_name,
      |f_city_id,f_city_name,f_region_id,f_region_name,f_terminal,
      |concat_ws(',',collect_set(f_channel_type)) as f_channel_type,
      |f_channel_id,f_channel_name,
      |cast(sum(f_play_time) as double) as f_play_time,
      |sum(f_play_count) as f_play_count
      |from t_chinel_program_live
      |group by f_date,f_hour,f_timerange,f_terminal,f_channel_id,f_channel_name,
      |f_region_id,f_region_name,f_province_id,f_province_name,f_city_id,f_city_name
    """.stripMargin
    val channelDF = hiveContext.sql(channel)

    val channelResultDF = channelDF.join(shareCount, channelDF("f_region_id") === shareCount("f_region_id_share")
      && channelDF("f_hour") === shareCount("f_hour_share") && channelDF("f_timerange") === shareCount("f_timerange_share")
      && channelDF("f_terminal") === shareCount("f_terminal_share") && channelDF("f_channel_id") === shareCount("f_channel_id_share"),
      "left_outer")
      .selectExpr("f_date", "f_hour", "f_timerange", "f_province_id",
        "f_province_name", "f_city_id", "f_city_name", "f_region_id", "f_region_name", "f_terminal",
        "f_channel_type", "f_channel_id", "f_channel_name", "f_play_time", "f_play_count", "nvl(f_share_count,0) as f_share_count", "f_play_user_amt")
    DBUtils.saveDataFrameToPhoenixNew(channelResultDF, Tables.T_CHANNEL_LIVE_HALFHOUR)
  }

  /**
    * 清表
    *
    * @param date
    */
  def delMysql(date: String): Unit = {
    delMysql(date, Tables.t_meizi_statistics_terminal_timerange)
    delMysql(date, Tables.t_meizi_statistics_terminal_channel)
  }

  /**
    * 防止重跑导致重复数据
    **/
  def delMysql(day: String, table: String): Unit = {
    val del_sql = s"delete from $table where f_date='$day'"
    DBUtils.executeSql(DBProperties.JDBC_URL, DBProperties.USER, DBProperties.PASSWORD, del_sql)
  }

  /**
    * 结果保存至mysql
    *
    * @param dataFrame
    * @param table
    */
  def saveToMysql(dataFrame: DataFrame, table: String) = {
    DBUtils.saveToHomedData_2(dataFrame, table)
  }


  /**
    * 从频道节目播放历史输数据中获取30分，60分时刻频道所播放节目
    *
    * @param sparkSession
    * @param dataFrame
    * @return
    */
  def processProgramDF(sparkSession: SparkSession, dataFrame: DataFrame) = {
    import sparkSession.sqlContext.implicits._
    dataFrame.map(row => {
      val listBuffer = new ListBuffer[(Long, Int, String, Int, Int)]
      val channelId = row.getAs[Long]("f_homed_channel_id")
      val programId = row.getAs[Int]("f_program_id")
      val programName = row.getAs[String]("f_program_name")

      val startTime = row.getAs[String]("f_program_start_time")
      val endTime = row.getAs[String]("f_program_end_time")
      val startHour = DateUtils.getHourFromDateTime(startTime)
      val startMinute = DateUtils.getMinuteFromDateTime(startTime)
      val endHour = DateUtils.getHourFromDateTime(endTime)
      val endMinute = DateUtils.getMinuteFromDateTime(endTime)
      //相差小时数
      val hnum = endHour.toInt - startHour.toInt

      if (hnum > 0) {
        //跨小时
        for (num <- 0 to (hnum)) {
          if (num == hnum) {
            if (endMinute.toInt >= 30) {
              listBuffer += ((channelId, programId, programName, startHour.toInt + num, 30))
            }
            //            listBuffer+=((channelId,programId,programName,startHour.toInt+num-1,60))
          } else {
            if (num == 0 && startMinute.toInt > 30) {
              listBuffer += ((channelId, programId, programName, startHour.toInt, 60))
            } else {
              listBuffer += ((channelId, programId, programName, startHour.toInt + num, 30))
              listBuffer += ((channelId, programId, programName, startHour.toInt + num, 60))
            }
          }
        }
      } else if (hnum < 0) {
        //跨天
        listBuffer += ((channelId, programId, programName, startHour.toInt, 60))
      } else {
        if (startMinute.toInt < 30 && endMinute.toInt > 30) {
          listBuffer += ((channelId, programId, programName, startHour.toInt, 30))
        }
      }
      listBuffer.toList
    }).flatMap(x => x).map(x => {
      ProgranPlayInfo(x._1, x._2, x._3, x._4, x._5)
    }).toDF
  }

  /**
    * 保存每天用户ID集合
    *
    * @param date
    * @param sqlContext
    * @param partAmt
    */
  def saveUserArray(date: String, sqlContext: HiveContext, partAmt: Int) = {
    import sqlContext.implicits._

    val sqlLive =
      s"""
         |select userId as f_user_id,
         |cast(deviceType as int) as f_terminal,
         |regionId as f_region_id,
         |nvl(cast(serviceId as string),'${GatherType.UNKNOWN_CHANNEL}') as f_channel_id,
         |playtime as f_play_time
         |from ${Tables.ORC_VIDEO_PLAY}
         |where day=$date and playType='${GatherType.LIVE_NEW}' --and playtime > 0 and serviceId is not null
      """.stripMargin
    sqlContext.sql("use bigdata")
    //分享量
    sqlContext.sql(
      s"""
         |select regionId as f_region_id_share,
         |cast(deviceType AS INT) as f_terminal_share,
         |exts['ID']  as f_channel_id_share,
         |count(1) as f_share_count
         |from ${Tables.ORC_USER_BEHAVIOR} sh
         |where day=$date and reportType='ShareSuccess'
         |group by regionId,deviceType,exts['ID']
       """.stripMargin).registerTempTable("t_share_tmp")
    sqlContext.cacheTable("t_share_tmp")
    sqlContext.sql(sqlLive)
      .map(x => {
        //        val date = x.getAs[String]("f_date")
        val terminal = x.getAs[Int]("f_terminal")
        val userId = x.getAs[String]("f_user_id")
        val regionId = x.getAs[String]("f_region_id")
        val channelId = x.getAs[String]("f_channel_id")
        val playTime = x.getAs[Long]("f_play_time")
        val key = terminal + "," + regionId + "," + channelId
        val value = (Set(userId), playTime, 1L)
        (key, value)
      }).reduceByKey((x, y) => {
      val playTime = x._2 + y._2
      val userSet = x._1 ++ y._1
      val playCnt = x._3 + y._3
      (userSet, playTime, playCnt)
    }).map(x => {
      val keyArr = x._1.split(",")
      val terminal = keyArr(0).toInt
      val regionId = keyArr(1)
      val channelId = keyArr(2)
      val userArray = x._2._1.toArray
      val playTime = x._2._2.toDouble //小时
      val playCount = x._2._3
      UserArray(regionId, terminal, channelId, userArray, playTime, playCount)
    }).repartition(partAmt).toDF().registerTempTable("t_user_array_tmp")
    sqlContext.sql(
      s"""
         |insert overwrite table ${Tables.T_USER_ARRAY_BY_DAY} partition(date='$date')
         |select uat.*,st.f_share_count
         |from t_user_array_tmp uat
         |left join t_share_tmp st on st.f_region_id_share=uat.f_region_id and st.f_terminal_share=uat.f_terminal and st.f_channel_id_share=uat.f_channel_id
      """.stripMargin)
    sqlContext.uncacheTable("t_share_tmp")
  }

  /**
    * added@20191218
    * 频道用户数及播放时长统计
    * 保存每天聚合后的userid集合、播放时长、
    *
    * @param startDate
    * @param endDate
    * @return
    */
  def getChannelUserCountNew(countType:String,startDate: String, endDate: String, sqlContext: HiveContext, sparkSession: SparkSession, partAmt: Int,regionDF: DataFrame): DataFrame = {
    //
    import sqlContext.implicits._
    sqlContext.sql("use bigdata")
    var countDate = startDate
    var channelSourceTab = ""//不同的统计类型查不同的基础表
    var columnName = "day"
    countType match {
      case "DAY" => channelSourceTab = "userprofile.t_user_basic_by_day"
      case "WEEK" => {
        channelSourceTab =  "userprofile.t_user_basic_by_week"
        columnName = "week"
      }
      case "MONTH" => {
        channelSourceTab = "userprofile.t_user_basic_by_month"
        columnName = "month"
        countDate = DateUtils.dateStrToDateTime(endDate,DateUtils.YYYYMMDD).toString(DateUtils.YYYYMM)
      }
      case "QUARTER" => channelSourceTab = "userprofile.t_user_basic_by_quarter"
      case "YEAR" => {
        channelSourceTab = "userprofile.t_user_basic_by_year"
        columnName = "year"
        countDate = DateUtils.dateStrToDateTime(endDate,DateUtils.YYYYMMDD).toString(DateUtils.YYYY)
      }
      case "7DAYS" => {
        channelSourceTab = "userprofile.t_user_basic_by_history"
      }
      case "30DAYS" => channelSourceTab = "userprofile.t_user_basic_by_history"
      case "1YEAR" => channelSourceTab = "userprofile.t_user_basic_by_history"
      case _ => "-1"
    }
    val channelUserSql =
      s"""
         |select f_region_id,f_terminal,case when f_channel_id='' or f_channel_id is null then '-1' else f_channel_id end as f_channel_id,count(distinct f_user_id) as f_user_count,
         |sum(f_play_time) as f_play_time,sum(f_play_count) as f_play_count,sum(nvl(f_share_count,0)) as f_share_count, judge_channel_category(f_channel_id) as f_channel_category
         |from ${channelSourceTab}
         |where $columnName =$countDate and f_play_type='live'
         |group by f_region_id,f_terminal,f_channel_id
      """.stripMargin
    println(channelUserSql)
    val chnnCntDf = sqlContext.sql(channelUserSql)
      .map(x => {
        val regionId = x.getAs[String]("f_region_id")
        val terminal = x.getAs[String]("f_terminal").toInt
        val channelId = x.getAs[String]("f_channel_id")
        val userCount = x.getAs[Long]("f_user_count").toInt
        val playTime = x.getAs[Long]("f_play_time").toDouble/3600
        val playCount = x.getAs[Long]("f_play_count")
        val shareCount = x.getAs[Long]("f_share_count")
        ChannelCnt(startDate, endDate, regionId, terminal, channelId, userCount, playTime, shareCount, playCount)
      }).repartition(partAmt).toDF
    val channelSubTypeDF = getChannelTypeDf(sparkSession, sqlContext)
//    val regionDF = getRegionDf(sparkSession, sqlContext)
    val df = chnnCntDf.join(channelSubTypeDF, chnnCntDf("f_channel_id") === channelSubTypeDF("chanel_id"), "left_outer").drop("chanel_id")
    val channelResultDF = df.join(regionDF, df("f_region_id") === regionDF("f_area_id"), "left_outer").drop("f_area_id")
    channelResultDF
  }
  /**
    * 频道用户数及播放时长统计
    * 保存每天聚合后的userid集合、播放时长、
    *
    * @param startDate
    * @param endDate
    * @return
    */
  def getChannelUserCount(startDate: String, endDate: String, sqlContext: HiveContext, sparkSession: SparkSession, partAmt: Int,regionDF:DataFrame): DataFrame = {
    import sqlContext.implicits._
    sqlContext.sql("use bigdata")
    val channelUserSql =
      s"""
         |select f_region_id,f_terminal,case when f_channel_id='' or f_channel_id is null then '-1' else f_channel_id end as f_channel_id,f_user_array,f_play_time,f_play_count,nvl(f_share_count,0) as f_share_count
         |from ${Tables.T_USER_ARRAY_BY_DAY}
         |where date >= '$startDate' and date <= '$endDate'
      """.stripMargin
    val chnnCntDf = sqlContext.sql(channelUserSql)
      .map(x => {
        val regionId = x.getAs[String]("f_region_id")
        val terminal = x.getAs[Byte]("f_terminal").toInt
        val channelId = x.getAs[String]("f_channel_id")
        val userArray = x.getAs[Seq[String]]("f_user_array")
        val playTime = x.getAs[Double]("f_play_time")
        val playCount = x.getAs[Long]("f_play_count")
        val shareCount = x.getAs[Long]("f_share_count")
        val userSet = userArray.toSet
        val key = regionId + "," + terminal + "," + channelId
        val value = (userSet, playTime, playCount, shareCount)
        (key, value)
      }).reduceByKey((x, y) => {
      (x._1 ++ y._1, x._2 + y._2, x._3 + y._3, x._4 + x._4)
    }).map(x => {
      val keyArr = x._1.split(",")
      val userSet = x._2._1
      val playTime = x._2._2
      val playCount = x._2._3
      val shareCount = 0L //x._2._4
      val regionId = keyArr(0)
      val terminal = keyArr(1).toInt
      val channelId = keyArr(2)
      val userCount = userSet.size
      ChannelCnt(startDate, endDate, regionId, terminal, channelId, userCount, playTime, shareCount, playCount)
    }).repartition(partAmt).toDF
    val channelSubTypeDF = getChannelTypeDf(sparkSession, sqlContext)
//    val regionDF = getRegionDf(sparkSession, sqlContext)
    val df = chnnCntDf.join(channelSubTypeDF, chnnCntDf("f_channel_id") === channelSubTypeDF("chanel_id"), "left_outer").drop("chanel_id")
    val channelResultDF = df.join(regionDF, df("f_region_id") === regionDF("f_area_id"), "left_outer").drop("f_area_id")
    channelResultDF
  }

  /**
    * 获取频道分类
    *
    * @param sparkSession
    * @param sqlContext
    * @return
    */
  def getChannelTypeDf(sparkSession: SparkSession, sqlContext: HiveContext): DataFrame = {
    import sqlContext.implicits._
    val temMap = new mutable.HashMap[String, String]()
    val channel =
      """
        |(SELECT DISTINCT CAST(channel_id AS CHAR) AS chanel_id,chinese_name AS f_channel_name,f_subtype
        |FROM channel_store
        |UNION
        |SELECT DISTINCT CAST(f_monitor_id AS CHAR) chanel_id ,f_monitor_name AS f_channel_name,f_sub_type AS f_subtype
        |FROM t_monitor_store) as channel_store
      """.stripMargin
    val channelDF = DBUtils.loadMysql(sqlContext, channel, DBProperties.JDBC_URL_DTVS, DBProperties.USER_DTVS, DBProperties.PASSWORD_DTVS)
    // 将频道分类 1-标清，2-高清，3-央视，4-地方（本地），5-其他
    val subTypeInfo =
      """
        |(select (case  when  f_name like '%标清%' then '1'
        |when f_name like '%高清%' then '2'
        |when f_name like '%央视%' then '3'
        |when (f_name like '%本地%' or f_name like '%地方%') then '4'
        |else '5' end) as f_channel_type,
        |f_original_id from t_media_type_info where f_status=5
        |) as channelType
      """.stripMargin
    val channelTypeDF = DBUtils.loadMysql(sqlContext, subTypeInfo, DBProperties.JDBC_URL_DTVS, DBProperties.USER_DTVS, DBProperties.PASSWORD_DTVS)
    channelTypeDF.registerTempTable("t_channel_type_info")
    val sql =
      """
        |select f_channel_type,concat_ws(',',collect_set(f_original_id)) as f_original_ids
        |from t_channel_type_info
        |group by f_channel_type
      """.stripMargin
    val typeDF = sparkSession.sqlContext.sql(sql)

    typeDF.collect().foreach(row => {
      val channelType = row.getAs[String]("f_channel_type")
      val channelOriginals = row.getAs[String]("f_original_ids")

      temMap += (channelOriginals -> channelType)
    })

    val channelTypeBC = sparkSession.sparkContext.broadcast(temMap) // @lixfy 所以说这里为什么要用一个广播变量
    channelDF.map(row => {
      val channelId = row.getAs[String]("chanel_id")
      val channelName = row.getAs[String]("f_channel_name")
      val subType = if (StringUtils.isNullOrEmpty(row.getAs[String]("f_subtype"))) "" else row.getAs[String]("f_subtype")
      val subTypeArr = subType.split("\\|")
      val buffer = new StringBuffer()
      val map = channelTypeBC.value

      for (subType <- subTypeArr) {
        val keys = map.keys
        for (key <- keys) {
          if (key.contains(subType) && !buffer.toString.contains(map.get(key).get)) {
            buffer.append(map.get(key).get).append(",")
          }
        }
      }

      var channelType = buffer.toString
      //如果频道找不到对应的类型，默认设置为5
      channelType = channelType match {
        case _ if (channelType.endsWith(",")) => channelType.substring(0, channelType.lastIndexOf(","))
        case _ if (channelType == null || channelType.equals("")) => "5"
        case _ => channelType
      }  // @lixfy 所以这里就是将频道类型的最后一个逗号去掉，然后将找不到类型的设置为5
      if (subType == "") {
        channelType = "5"
      }
      ChannelSubType(channelId, channelName, channelType)
    }).toDF()
  }
/*
  /**
    * 区域信息
    *
    * @param sparkSession
    * @param sqlContext
    */
  def getRegionDf(sparkSession: SparkSession, sqlContext: HiveContext) = {
    //项目部署地code(省或者地市)
    val regionCode = RegionUtils.getRootRegion
    val regionBC: Broadcast[String] = sparkSession.sparkContext.broadcast(regionCode)
    //区域信息表
    val region =
      s"""
         |(select cast(a.area_id as char) as f_area_id,a.area_name as f_region_name,
         |cast(a.city_id as char) as f_city_id,c.city_name as f_city_name,
         |cast(c.province_id as char) as f_province_id,p.province_name as f_province_name
         |from (area a left join city c on a.city_id=c.city_id)
         |left join province p on c.province_id=p.province_id
         |where a.city_id=${regionBC.value} or c.province_id=${regionBC.value}
         |) as region
        """.stripMargin
    DBUtils.loadMysql(sqlContext, region, DBProperties.JDBC_URL_IUSM, DBProperties.USER_IUSM, DBProperties.PASSWORD_IUSM)
  }*/

  /**
    * 频道年统计-需每天按月累加统计
    * @param sqlContext
    * @param date
    */
  def channelLiveByYear(sqlContext:HiveContext,date:String)={
    import sqlContext.implicits._
    var startDate = date
    var endDate = date
    var year = ""
    if(date.length == 6){
      year = DateUtils.dateStrToDateTime(date,DateUtils.YYYYMM).toString(DateUtils.YYYY)
      startDate = DateUtils.dateStrToDateTime(date,DateUtils.YYYYMM).toString(DateUtils.YYYYMMDD)
      endDate = DateUtils.dateStrToDateTime(date,DateUtils.YYYYMM).plusMonths(1).plusDays(-1).toString(DateUtils.YYYYMMDD) // @lixfy 起始日期增加一个月再减掉一天
    }else if(date.length == 8){
      year = DateUtils.dateStrToDateTime(date,DateUtils.YYYYMMDD).toString(DateUtils.YYYY)
    }else{
      year = DateUtils.dateStrToDateTime(DateUtils.getNowDate(),DateUtils.YYYYMM).toString(DateUtils.YYYY)
    }

    val startMonth = DateUtils.dateStrToDateTime(year,DateUtils.YYYY).toString(DateUtils.YYYYMM)
    val endMonth = DateUtils.dateStrToDateTime(year,DateUtils.YYYY).plusYears(1).toString(DateUtils.YYYYMM)
    //    STB/CA等设备号信息获取
    //    val devNumDf = UserDeviceUtils.getDeviceNumDf(sqlContext)
    //年用户累加表
    sqlContext.sql("use bigdata")
    val yearUserSql =
      s"""
         |select region_id,da,device_id,terminal,channel_id
         |from ${Tables.T_CHANNEL_USER_BY_YEAR}
         |where year=$year
       """.stripMargin
    val yearHisUserDf = sqlContext.sql(yearUserSql)

    //统计当天/当月用户
    val dayUserSql =
      s"""
         |select regionid as region_id, userid as da, cast(deviceid as int) as device_id, deviceType as terminal,
         |nvl(cast(serviceId as string),'${GatherType.UNKNOWN_CHANNEL}') as channel_id
         |from ${Tables.ORC_VIDEO_PLAY}
         |where day>=$startDate and day<=$endDate and playType='${GatherType.LIVE_NEW}'
       """.stripMargin
    val dayPlayDf = sqlContext.sql(dayUserSql).distinct()
    //    dayPlayDf.show(1000,false)
    val yearUserDf = yearHisUserDf.unionAll(dayPlayDf) // <<<----------------- @lixf 所以这里为什么要这样做，不直接用 用户总量那里的那种方法
      .map(x => {                                                            //  另外，DataFrame的话，是保存了数据表的结构的，没必要再专门把这些字段拿出来吧
        val regionId = x.getAs[String]("region_id")
        val userId = x.getAs[String]("da")
        val deviceId = x.getAs[String]("device_id")
        val terminal = x.getAs[String]("terminal")
        val channelId = x.getAs[String]("channel_id")
        (regionId,userId,deviceId,terminal,channelId,year)
      }).toDF().distinct()
      .withColumnRenamed("_1","region_id")
      .withColumnRenamed("_2","da")
      .withColumnRenamed("_3","device_id")
      .withColumnRenamed("_4","terminal")
      .withColumnRenamed("_5","channel_id")
      .withColumnRenamed("_6","year")
    println("插入年频道用户积累表记录数：" + yearUserDf.count())
    //    yearUserDf.show(1000,false)
    if(!yearUserDf.rdd.isEmpty()){
      try{
        yearUserDf.repartition(partAmt).write.format("orc").mode(SaveMode.Overwrite).partitionBy("year").insertInto(Tables.T_CHANNEL_USER_BY_YEAR)
        println("插入年频道用户积累表成功" )
      }catch {
        case e:Exception => {
          println("插入年频道用户积累表失败" )
        }
      }
    }
    val yearUserCntDf = yearUserDf.drop("device_id").distinct().groupBy("region_id","terminal","channel_id")
      .agg("da"->"count").withColumnRenamed("count(da)","f_play_user_amt")
    //    yearUserCntDf.show(false)
    //取本年度所有月统计
    println("startMonth:" + startMonth)
    println("endMonth:" + endMonth)
    val allMonthPlaySql =
      s"""
         |(select f_date,f_province_id,f_province_name,f_city_id,f_city_name,f_region_id,f_region_name,f_terminal,
         |f_channel_type,f_channel_id,f_channel_name,f_play_time,f_play_count,f_share_count
         |from ${Tables.T_CHANNEL_LIVE_MONTH}
         |where f_date>='$startMonth' and f_date<'$endMonth') as allMonth
       """.stripMargin
    val loadAllMonthPlayDf = DBUtils.loadDataFromPhoenix2(sqlContext,allMonthPlaySql)
    println("allMonthPlayDf:" + loadAllMonthPlayDf.count())
    //    allMonthPlayDf.show(false)
    if(!loadAllMonthPlayDf.rdd.isEmpty()){
      val allMonthPlayDf = loadAllMonthPlayDf.groupBy("f_province_id","f_province_name","f_city_id","f_city_name","f_region_id","f_region_name","f_terminal",
        "f_channel_type", "f_channel_id", "f_channel_name")
        .agg("f_play_time" -> "sum","f_play_count" -> "sum","f_share_count" -> "sum")
        .withColumnRenamed("sum(f_play_time)","f_play_time")
        .withColumnRenamed("sum(f_play_count)","f_play_count")
        .withColumnRenamed("sum(f_share_count)","f_share_count")

      val yearChannelSaveDf = allMonthPlayDf.join(yearUserCntDf,allMonthPlayDf("f_region_id")===yearUserCntDf("region_id")
                                                              &&allMonthPlayDf("f_terminal")===yearUserCntDf("terminal")
                                                              &&allMonthPlayDf("f_channel_id")===yearUserCntDf("channel_id"))
        .selectExpr(s"'$year' as f_date","f_province_name","f_city_id","f_city_name","f_region_id","f_region_name","f_terminal",
          "f_channel_type", "f_channel_id", "f_channel_name","f_play_time","f_play_count","f_share_count","f_play_user_amt")

      //年统计 增加环比前期值@20191212
      val lastYear = DateUtils.dateStrToDateTime(date,DateUtils.YYYYMMDD).plusDays(-365).toString(DateUtils.YYYY)
      println("lastYear:" + lastYear)
      val lastYearSql = s"(select f_play_count,f_play_time,f_region_id,f_terminal,f_channel_id from ${Tables.T_CHANNEL_LIVE_YEAR} where f_date='$lastYear') as last_day"
      val lastYearDf = DBUtils.loadDataFromPhoenix2(sqlContext,lastYearSql)
        .selectExpr("f_play_count as f_play_count_early","f_play_time as f_play_time_early","f_region_id as l_region_id","f_terminal as l_terminal","f_channel_id as l_channel_id")

      val resultDf = yearChannelSaveDf.join(lastYearDf,yearChannelSaveDf("f_region_id")===lastYearDf("l_region_id")
                                         && yearChannelSaveDf("f_terminal")===lastYearDf("l_terminal")
                                         &&yearChannelSaveDf("f_channel_id")===lastYearDf("l_channel_id")
                                         , "left_outer")
        .selectExpr(s"f_date","f_province_name","f_city_id","f_city_name","f_region_id","f_region_name","f_terminal",
          "f_channel_type", "f_channel_id", "f_channel_name","f_play_time","f_play_count","f_share_count","f_play_user_amt",
          "nvl(f_play_count_early,0) as f_play_count_early","nvl(f_play_time_early,0) as f_play_time_early")

      try{
        println("插入年统计记录数：" + resultDf.count())
        DBUtils.saveDataFrameToPhoenixNew(resultDf,Tables.T_CHANNEL_LIVE_YEAR)
        println("保存年频道统计成功")
      }catch {
        case e:Exception => {
          println("保存年频道统计失败")
          e.printStackTrace()
        }
      }
    }
  }

  /**
   * @author lixfy
   * @date 2020-8-28
   *       通过channel_id获取该频道的频道类型（轮播频道、付费频道、。。。）
   */
  def getChannelCategoryTable(hiveContext: HiveContext): DataFrame = {
    val channel =
      s"""
         |(select channel_id as f_channel_id,chinese_name as channel_name,
         |f_subtype as channel_type
         |from channel_store) as t""".stripMargin
    DBUtils.loadMysql(hiveContext, channel, DBProperties.JDBC_URL_DTVS, DBProperties.USER_DTVS, DBProperties.PASSWORD_DTVS)
  }

  def channelCategoryMap(sparkSession: SparkSession, channelDF: DataFrame):Unit  = {
    channelTypeMap = channelDF
      .selectExpr("channel_id", "case when channel_type like '%10014002%' then 1 when channel_type like '%10010008%' then 2 else 0 end as channel_type"  )
      // 0: 直播频道 ； 1：轮播频道 ； 2：付费频道
      .rdd
      .map(row => row.getAs("channel_id").toString -> row.getAs("channel_type").toString)
      .collectAsMap()
      .asInstanceOf[mutable.HashMap[String, String]]
    val channelTypeCarouselBC = sparkSession.sparkContext.broadcast[mutable.HashMap[String, String]](channelTypeMap)
  }

  def channelCategoryProcess(sparkSession: SparkSession, hiveContext: HiveContext):Unit = {
    val channelDf = getChannelCategoryTable(hiveContext)
    channelCategoryMap(sparkSession, channelDf)
  }

  def judgeChannelCategory(channelId: String): String = {
    channelTypeMap.getOrElse(channelId, "-1")
  }

}
