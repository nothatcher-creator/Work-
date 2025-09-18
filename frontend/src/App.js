import React, { useState } from "react";
import "./App.css";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Sidebar from "./components/Layout/Sidebar";
import Header from "./components/Layout/Header";
import OverviewDashboard from "./components/Dashboard/OverviewDashboard";
import EmployeesView from "./components/Views/EmployeesView";
import ScheduleView from "./components/Views/ScheduleView";
import TasksView from "./components/Views/TasksView";
import ChatView from "./components/Views/ChatView";
import TimeClockView from "./components/Views/TimeClockView";

const Dashboard = () => {
  const [activeView, setActiveView] = useState('overview');
  const [userRole] = useState('Admin'); // This could come from auth context

  const renderView = () => {
    switch (activeView) {
      case 'overview':
        return <OverviewDashboard />;
      case 'users':
        return <EmployeesView />;
      case 'schedule':
        return <ScheduleView />;
      case 'tasks':
        return <TasksView />;
      case 'chat':
        return <ChatView />;
      case 'time-clock':
        return <TimeClockView />;
      default:
        return <OverviewDashboard />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar activeItem={activeView} onItemClick={setActiveView} />
      <div className="flex-1 flex flex-col">
        <Header userRole={userRole} />
        <main className="flex-1 p-6">
          {renderView()}
        </main>
      </div>
    </div>
  );
};

function App() {
  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Dashboard />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;