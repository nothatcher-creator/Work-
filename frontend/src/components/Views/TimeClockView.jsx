import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Clock, Play, Pause, MapPin, Calendar } from 'lucide-react';
import { mockData } from '../../data/mockData';

const TimeCard = ({ employee, isCurrentUser = false }) => {
  const [currentTime, setCurrentTime] = useState(new Date());
  
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const formatTime = (date) => {
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      second: '2-digit'
    });
  };

  return (
    <Card className={`${isCurrentUser ? 'ring-2 ring-blue-500 bg-blue-50' : ''} hover:shadow-md transition-all`}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <Avatar className="h-12 w-12">
              <AvatarImage src={employee.avatar} />
              <AvatarFallback>{employee.name.split(' ').map(n => n[0]).join('')}</AvatarFallback>
            </Avatar>
            <div>
              <h3 className="font-semibold text-gray-900">{employee.name}</h3>
              <p className="text-sm text-gray-600">{employee.department}</p>
            </div>
          </div>
          
          <Badge 
            variant={employee.clockedIn ? 'default' : 'secondary'}
            className={employee.clockedIn ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}
          >
            {employee.clockedIn ? 'Clocked In' : 'Clocked Out'}
          </Badge>
        </div>

        {isCurrentUser && (
          <div className="text-center mb-4">
            <div className="text-3xl font-bold text-blue-600 mb-2">
              {formatTime(currentTime)}
            </div>
            <p className="text-sm text-gray-600">Current Time</p>
          </div>
        )}

        <div className="space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-600">Today's Hours:</span>
            <span className="font-medium">7h 23m</span>
          </div>
          
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-600">Week Hours:</span>
            <span className="font-medium">35h 15m</span>
          </div>
          
          {employee.clockedIn && (
            <div className="flex items-center gap-1 text-sm text-green-600">
              <MapPin className="h-4 w-4" />
              <span>On Location</span>
            </div>
          )}
        </div>

        {isCurrentUser && (
          <div className="mt-6 flex gap-2">
            <Button 
              className={`flex-1 ${employee.clockedIn ? 'bg-red-600 hover:bg-red-700' : 'bg-green-600 hover:bg-green-700'}`}
            >
              {employee.clockedIn ? (
                <>
                  <Pause className="h-4 w-4 mr-2" />
                  Clock Out
                </>
              ) : (
                <>
                  <Play className="h-4 w-4 mr-2" />
                  Clock In
                </>
              )}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

const TimeStats = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Today's Overview</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-green-600">134</div>
            <p className="text-sm text-gray-600">Clocked In</p>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-red-600">4</div>
            <p className="text-sm text-gray-600">Running Late</p>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-600">24</div>
            <p className="text-sm text-gray-600">Scheduled</p>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-orange-600">1,247</div>
            <p className="text-sm text-gray-600">Total Hours</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

const RecentActivity = () => {
  const activities = [
    { user: "Rose Smith", action: "Clocked in", time: "10:04 AM", type: "clock-in" },
    { user: "Robert Fox", action: "Clocked out", time: "9:45 AM", type: "clock-out" },
    { user: "Theresa Webb", action: "Break started", time: "9:30 AM", type: "break" },
    { user: "Ronald Richards", action: "Clocked in", time: "9:00 AM", type: "clock-in" }
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Recent Activity</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {activities.map((activity, index) => (
            <div key={index} className="flex items-center justify-between py-2">
              <div className="flex items-center gap-3">
                <div className={`w-2 h-2 rounded-full ${
                  activity.type === 'clock-in' ? 'bg-green-500' : 
                  activity.type === 'clock-out' ? 'bg-red-500' : 'bg-orange-500'
                }`} />
                <div>
                  <p className="text-sm font-medium">{activity.user}</p>
                  <p className="text-xs text-gray-500">{activity.action}</p>
                </div>
              </div>
              <span className="text-xs text-gray-500">{activity.time}</span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

export const TimeClockView = () => {
  const employees = mockData.employees;
  const currentUser = employees[0]; // Assuming first employee is current user

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Time Clock</h1>
          <p className="text-gray-600">Track and manage employee time</p>
        </div>
        
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <Calendar className="h-4 w-4" />
          {new Date().toLocaleDateString('en-US', { 
            weekday: 'long', 
            year: 'numeric', 
            month: 'long', 
            day: 'numeric' 
          })}
        </div>
      </div>

      {/* Stats Overview */}
      <TimeStats />

      {/* Current User Time Card */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Your Time Clock</h2>
        <div className="max-w-md">
          <TimeCard employee={currentUser} isCurrentUser={true} />
        </div>
      </div>

      {/* Team Overview */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Team Status</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {employees.slice(1).map(employee => (
              <TimeCard key={employee.id} employee={employee} />
            ))}
          </div>
        </div>
        
        <div>
          <RecentActivity />
        </div>
      </div>
    </div>
  );
};

export default TimeClockView;