import React, { useState } from 'react';
import { 
  Home, 
  Users, 
  Clock, 
  Calendar, 
  CheckSquare, 
  MessageCircle, 
  BarChart3, 
  Settings, 
  BookOpen,
  HelpCircle,
  FileText
} from 'lucide-react';
import { cn } from '../../lib/utils';

const menuItems = [
  { id: 'overview', label: 'Overview', icon: Home, category: 'main' },
  { id: 'users', label: 'Users', icon: Users, category: 'main' },
  
  { id: 'time-clock', label: 'Time Clock', icon: Clock, category: 'operations' },
  { id: 'schedule', label: 'Schedule', icon: Calendar, category: 'operations' },
  { id: 'tasks', label: 'Quick Tasks', icon: CheckSquare, category: 'operations' },
  
  { id: 'chat', label: 'Chat', icon: MessageCircle, category: 'communication' },
  { id: 'updates', label: 'Updates', icon: BarChart3, category: 'communication' },
  { id: 'knowledge', label: 'Knowledge Base', icon: BookOpen, category: 'communication' },
  { id: 'help', label: 'Help Desk', icon: HelpCircle, category: 'communication' },
  
  { id: 'time-off', label: 'Time Off', icon: Clock, category: 'hr' },
  { id: 'training', label: 'Training', icon: BookOpen, category: 'hr' },
  { id: 'documents', label: 'Documents', icon: FileText, category: 'hr' },
];

const categories = {
  main: '',
  operations: 'Operations',
  communication: 'Communication',
  hr: 'HR & Skills'
};

export const Sidebar = ({ activeItem, onItemClick }) => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const renderMenuItem = (item) => {
    const Icon = item.icon;
    const isActive = activeItem === item.id;
    
    return (
      <button
        key={item.id}
        onClick={() => onItemClick(item.id)}
        className={cn(
          "w-full flex items-center gap-3 px-3 py-2.5 text-sm font-medium rounded-lg transition-all duration-200 hover:bg-blue-50 hover:text-blue-700",
          isActive 
            ? "bg-blue-100 text-blue-700 border-r-2 border-blue-600" 
            : "text-gray-600 hover:text-gray-900"
        )}
      >
        <Icon className="h-5 w-5 flex-shrink-0" />
        {!isCollapsed && <span>{item.label}</span>}
      </button>
    );
  };

  return (
    <div className={cn(
      "bg-white border-r border-gray-200 flex flex-col transition-all duration-300",
      isCollapsed ? "w-16" : "w-64"
    )}>
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">C</span>
          </div>
          {!isCollapsed && <span className="font-bold text-xl text-gray-900">connecteam</span>}
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
        {Object.entries(categories).map(([categoryKey, categoryLabel]) => {
          const categoryItems = menuItems.filter(item => item.category === categoryKey);
          
          return (
            <div key={categoryKey} className="space-y-1">
              {categoryLabel && !isCollapsed && (
                <h3 className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  {categoryLabel}
                </h3>
              )}
              {categoryItems.map(renderMenuItem)}
              {categoryKey !== 'hr' && <div className="h-2" />}
            </div>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-gray-200">
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="w-full flex items-center gap-3 px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-lg transition-colors"
        >
          <Settings className="h-5 w-5" />
          {!isCollapsed && <span>Settings</span>}
        </button>
      </div>
    </div>
  );
};

export default Sidebar;