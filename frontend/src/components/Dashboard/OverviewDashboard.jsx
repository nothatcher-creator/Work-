import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Progress } from '../ui/progress';
import { Clock, Users, CheckSquare, TrendingUp, Calendar, MessageCircle } from 'lucide-react';
import { mockData } from '../../data/mockData';

const MetricCard = ({ title, value, subtitle, icon: Icon, trend, color = "blue" }) => {
  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">{title}</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
            {subtitle && <p className="text-sm text-gray-500 mt-1">{subtitle}</p>}
          </div>
          <div className={`w-12 h-12 bg-${color}-100 rounded-lg flex items-center justify-center`}>
            <Icon className={`h-6 w-6 text-${color}-600`} />
          </div>
        </div>
        {trend && (
          <div className="mt-4 flex items-center text-sm">
            <TrendingUp className="h-4 w-4 text-green-500 mr-1" />
            <span className="text-green-600 font-medium">{trend}</span>
            <span className="text-gray-500 ml-1">from last week</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

const ActivityFeed = () => {
  return (
    <Card className="col-span-1">
      <CardHeader>
        <CardTitle className="text-lg">Daily Activity</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {mockData.activities.map((activity, index) => (
            <div key={index} className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0">
                <img 
                  src={activity.avatar} 
                  alt={activity.user}
                  className="w-8 h-8 rounded-full"
                />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-900">
                  <span className="font-medium">{activity.user}</span> {activity.action}
                </p>
                <p className="text-xs text-gray-500">{activity.time}</p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

const EngagementChart = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Team Engagement</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium">Users read your update</span>
            <span className="text-2xl font-bold text-blue-600">88%</span>
          </div>
          <Progress value={88} className="h-2" />
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-600">92<span className="text-gray-400">/104</span></div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

const QuickStats = () => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card className="bg-purple-50 border-purple-200">
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-purple-600">Running late</p>
              <p className="text-2xl font-bold text-purple-900">4</p>
            </div>
            <Clock className="h-8 w-8 text-purple-600" />
          </div>
        </CardContent>
      </Card>
      
      <Card className="bg-blue-50 border-blue-200">
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-blue-600">Clocked in</p>
              <p className="text-2xl font-bold text-blue-900">134</p>
            </div>
            <Users className="h-8 w-8 text-blue-600" />
          </div>
        </CardContent>
      </Card>
      
      <Card className="bg-green-50 border-green-200">
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-green-600">Scheduled shifts</p>
              <p className="text-2xl font-bold text-green-900">24</p>
            </div>
            <Calendar className="h-8 w-8 text-green-600" />
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export const OverviewDashboard = () => {
  return (
    <div className="space-y-6">
      {/* Quick Stats */}
      <QuickStats />
      
      {/* Main Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          title="Total Employees"
          value="156"
          subtitle="5 new this month"
          icon={Users}
          trend="+12%"
          color="blue"
        />
        <MetricCard
          title="Hours Tracked"
          value="1,247"
          subtitle="This week"
          icon={Clock}
          trend="+8%"
          color="green"
        />
        <MetricCard
          title="Tasks Completed"
          value="89"
          subtitle="Out of 124"
          icon={CheckSquare}
          trend="+15%"
          color="purple"
        />
        <MetricCard
          title="Messages Sent"
          value="2,341"
          subtitle="This month"
          icon={MessageCircle}
          trend="+23%"
          color="orange"
        />
      </div>

      {/* Charts and Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <EngagementChart />
        </div>
        <ActivityFeed />
      </div>
    </div>
  );
};

export default OverviewDashboard;