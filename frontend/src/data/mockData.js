export const mockData = {
  activities: [
    {
      user: "Rose Smith",
      action: "clocked in",
      time: "01/20/25 at 10:04am",
      avatar: "https://images.unsplash.com/photo-1494790108755-2616b612c99e?w=32&h=32&fit=crop&crop=face"
    },
    {
      user: "Robert Fox",
      action: "requested Time off",
      time: "01/18/25 at 3:01pm",
      avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=32&h=32&fit=crop&crop=face"
    },
    {
      user: "Theresa Webb",
      action: "edited policy settings for Paid leave",
      time: "01/18/25 at 10:30am",
      avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=32&h=32&fit=crop&crop=face"
    },
    {
      user: "Ronald Richards",
      action: "requested Time off",
      time: "01/18/25 at 9:00am",
      avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=32&h=32&fit=crop&crop=face"
    }
  ],
  
  employees: [
    {
      id: 1,
      name: "John Doe",
      role: "Manager",
      department: "Operations",
      status: "active",
      email: "john.doe@company.com",
      clockedIn: true,
      avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=40&h=40&fit=crop&crop=face"
    },
    {
      id: 2,
      name: "Sarah Smith",
      role: "Team Lead",
      department: "Sales",
      status: "active",
      email: "sarah.smith@company.com",
      clockedIn: true,
      avatar: "https://images.unsplash.com/photo-1494790108755-2616b612c99e?w=40&h=40&fit=crop&crop=face"
    },
    {
      id: 3,
      name: "Mike Johnson",
      role: "Developer",
      department: "Engineering",
      status: "offline",
      email: "mike.johnson@company.com",
      clockedIn: false,
      avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=40&h=40&fit=crop&crop=face"
    },
    {
      id: 4,
      name: "Emma Wilson",
      role: "Designer", 
      department: "Design",
      status: "active",
      email: "emma.wilson@company.com",
      clockedIn: true,
      avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=40&h=40&fit=crop&crop=face"
    }
  ],

  tasks: [
    {
      id: 1,
      title: "Complete project documentation",
      assignee: "John Doe",
      dueDate: "2025-01-25",
      priority: "high",
      status: "in-progress",
      department: "Engineering"
    },
    {
      id: 2,
      title: "Review quarterly reports",
      assignee: "Sarah Smith",
      dueDate: "2025-01-23",
      priority: "medium",
      status: "pending",
      department: "Sales"
    },
    {
      id: 3,
      title: "Update team schedules",
      assignee: "Mike Johnson",
      dueDate: "2025-01-24",
      priority: "low",
      status: "completed",
      department: "Operations"
    },
    {
      id: 4,
      title: "Design new user interface",
      assignee: "Emma Wilson",
      dueDate: "2025-01-26",
      priority: "high",
      status: "in-progress",
      department: "Design"
    }
  ],

  schedules: [
    {
      id: 1,
      employee: "John Doe",
      date: "2025-01-20",
      shift: "9:00 AM - 5:00 PM",
      department: "Operations",
      status: "confirmed"
    },
    {
      id: 2,
      employee: "Sarah Smith", 
      date: "2025-01-20",
      shift: "10:00 AM - 6:00 PM",
      department: "Sales",
      status: "confirmed"
    },
    {
      id: 3,
      employee: "Mike Johnson",
      date: "2025-01-20", 
      shift: "8:00 AM - 4:00 PM",
      department: "Engineering",
      status: "pending"
    }
  ],

  messages: [
    {
      id: 1,
      sender: "John Doe",
      content: "Team meeting at 2 PM today",
      timestamp: "10:30 AM",
      type: "announcement",
      avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=32&h=32&fit=crop&crop=face"
    },
    {
      id: 2,
      sender: "Sarah Smith",
      content: "Great work on the project everyone!",
      timestamp: "9:45 AM", 
      type: "message",
      avatar: "https://images.unsplash.com/photo-1494790108755-2616b612c99e?w=32&h=32&fit=crop&crop=face"
    },
    {
      id: 3,
      sender: "System",
      content: "New company policy updated",
      timestamp: "8:00 AM",
      type: "system",
      avatar: null
    }
  ]
};

export default mockData;