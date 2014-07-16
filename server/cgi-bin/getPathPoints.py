#!/usr/bin/python
# -*- coding: UTF-8 -*-

# enable debugging
import cgitb
cgitb.enable()
import cgi
form = cgi.FieldStorage()
import MySQLdb
import json

class PointGroup:
    def __init__(self, id):
        self.pathId=id
        self.points=[]
    def addPoint(self,point):
        self.points.append(point)

class Point:
    def __init__(self, lat, lon):
        self.latitude=lat
        self.longitude=lon

pathId=form.getvalue("pathid")
pointGroup=PointGroup(pathId)

conn = MySQLdb.connect (host = "localhost",
			user = "thetraj8_admin",
			passwd = "sincap",
			db = "thetraj8_followme")
cursor = conn.cursor()
query = ("SELECT lat, lon FROM point where parentid=%s and pointtype=1 order by pointid")
cursor.execute(query, (pathId))
for (lat, lon) in cursor:
    p = Point(lat,lon)
    pointGroup.addPoint(p)

print "Content-Type: text/plain;charset=utf-8"
print

print json.dumps(pointGroup, default=lambda o:o.__dict__)

cursor.close()
conn.close()