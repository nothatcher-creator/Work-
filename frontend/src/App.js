import React, { useState, useEffect } from "react";
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
import BusinessSettings from "./components/Settings/BusinessSettings";
import InitialSetup from "./components/Onboarding/InitialSetup";
import { Toaster } from "./components/ui/toaster";

const Dashboard = ({ business, userRole }) => {
  const [activeView, setActiveView] = useState('overview');

  const renderView = () => {
    switch (activeView) {
      case 'overview':
        return <OverviewDashboard business={business} userRole={userRole} />;
      case 'users':
        return <EmployeesView business={business} userRole={userRole} />;
      case 'schedule':
        return <ScheduleView business={business} userRole={userRole} />;
      case 'tasks':
        return <TasksView business={business} userRole={userRole} />;
      case 'chat':
        return <ChatView business={business} userRole={userRole} />;
      case 'time-clock':
        return <TimeClockView business={business} userRole={userRole} />;
      case 'settings':
        return <BusinessSettings userRole={userRole} />;
      default:
        return <OverviewDashboard business={business} userRole={userRole} />;
    }
  };

  // Filter enabled features based on business settings
  const isFeatureEnabled = (feature) => {
    return business?.features?.[feature] === true;
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar 
        activeItem={activeView} 
        onItemClick={setActiveView}
        businessName={business?.name}
        enabledFeatures={business?.features || {}}
      />
      <div className="flex-1 flex flex-col">
        <Header userRole={userRole} businessName={business?.name} />
        <main className="flex-1 p-6">
          {renderView()}
        </main>
      </div>
    </div>
  );
};

function App() {
  const [isInitialSetup, setIsInitialSetup] = useState(false);
  const [business, setBusiness] = useState(null);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user has already set up business
    const existingBusiness = localStorage.getItem('currentBusiness');
    const existingRole = localStorage.getItem('userRole');
    
    if (existingBusiness && existingRole) {
      setBusiness(JSON.parse(existingBusiness));
      setUserRole(existingRole);
      setIsInitialSetup(false);
    } else {
      setIsInitialSetup(true);
    }
    
    setLoading(false);
  }, []);

  const handleSetupComplete = (businessData, role) => {
    setBusiness(businessData);
    setUserRole(role);
    setIsInitialSetup(false);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
          </div>
          <p className="text-gray-600">Loading Connecteam...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route 
            path="/" 
            element={
              business && userRole ? (
                <Dashboard business={business} userRole={userRole} />
              ) : (
                <div className="min-h-screen bg-gray-50">
                  <InitialSetup 
                    isOpen={isInitialSetup} 
                    onComplete={handleSetupComplete} 
                  />
                </div>
              )
            } 
          />
        </Routes>
      </BrowserRouter>
      <Toaster />
    </div>
  );
}

export default App;