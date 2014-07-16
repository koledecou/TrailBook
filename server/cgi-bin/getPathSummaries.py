#!/usr/bin/python
# -*- coding: UTF-8 -*-

# enable debugging
import cgitb
cgitb.enable()
import MySQLdb
import json

class Point:
    def __init__(self, lat, lon):
        self.latitude=lat
        self.longitude=lon

class PathSummary:
    def __init__(self, id, name, desc, creatorName, startPoint, endPoint):
        self.id=id
        self.name=name
        self.description=desc
        self.start=startPoint
        self.end=endPoint

summaries=[]

conn = MySQLdb.connect (host = "localhost",
			user = "thetraj8_admin",
			passwd = "sincap",
			db = "thetraj8_followme")
cursor = conn.cursor()
query = ("SELECT pathid, "
	    "pathname, "
	    "description, "
	    "creatorname, "
	    "(select lat from point where pointid=startpointid) startlat,"
	    "(select lon from point where pointid=startpointid) startlon,"
	    "(select lat from point where pointid=endpointid) endlat,"
	    "(select lon from point where pointid=endpointid) endlon FROM path "
	    "where categoryid=6")
cursor.execute(query)
for (pathid, pathname, description, creatorname, startlat, startlon, endlat, endlon) in cursor:
    summary=PathSummary(pathid, pathname, description, creatorname, Point(startlat,startlon), Point(endlat, endlon))
    summaries.append(summary)


print "Content-Type: text/plain;charset=utf-8"
print
print json.dumps(summaries, default=lambda o:o.__dict__)

cursor.close()
conn.close()