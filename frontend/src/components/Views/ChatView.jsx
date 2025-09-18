import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Textarea } from '../ui/textarea';
import { 
  Send, 
  Search, 
  Plus, 
  MessageCircle, 
  Users, 
  Bell,
  MoreHorizontal,
  Paperclip,
  Smile
} from 'lucide-react';
import { mockData } from '../../data/mockData';

const ChatSidebar = ({ selectedChat, onSelectChat }) => {
  const chats = [
    {
      id: 1,
      name: "General",
      type: "channel",
      members: 156,
      lastMessage: "Team meeting at 2 PM today",
      lastTime: "10:30 AM",
      unread: 3,
      avatar: null
    },
    {
      id: 2,
      name: "Sarah Smith",
      type: "direct",
      lastMessage: "Great work on the project!",
      lastTime: "9:45 AM",
      unread: 1,
      avatar: "https://images.unsplash.com/photo-1494790108755-2616b612c99e?w=32&h=32&fit=crop&crop=face"
    },
    {
      id: 3,
      name: "Engineering Team",
      type: "channel",
      members: 12,
      lastMessage: "Code review is ready",
      lastTime: "Yesterday",
      unread: 0,
      avatar: null
    },
    {
      id: 4,
      name: "Mike Johnson",
      type: "direct",
      lastMessage: "Can we schedule a call?",
      lastTime: "Yesterday",
      unread: 0,
      avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=32&h=32&fit=crop&crop=face"
    }
  ];

  return (
    <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Messages</h2>
          <Button size="sm" variant="outline">
            <Plus className="h-4 w-4" />
          </Button>
        </div>
        
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
          <Input placeholder="Search conversations..." className="pl-10" />
        </div>
      </div>
      
      <div className="flex-1 overflow-y-auto">
        {chats.map(chat => (
          <div
            key={chat.id}
            onClick={() => onSelectChat(chat)}
            className={`p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors ${
              selectedChat?.id === chat.id ? 'bg-blue-50 border-l-4 border-l-blue-500' : ''
            }`}
          >
            <div className="flex items-start gap-3">
              <div className="relative">
                <Avatar className="h-10 w-10">
                  {chat.avatar ? (
                    <AvatarImage src={chat.avatar} />
                  ) : (
                    <AvatarFallback>
                      {chat.type === 'channel' ? '#' : chat.name.split(' ').map(n => n[0]).join('')}
                    </AvatarFallback>
                  )}
                </Avatar>
                {chat.type === 'direct' && (
                  <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-green-500 rounded-full border-2 border-white" />
                )}
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 truncate">{chat.name}</span>
                    {chat.type === 'channel' && (
                      <span className="text-xs text-gray-500">({chat.members})</span>
                    )}
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-xs text-gray-500">{chat.lastTime}</span>
                    {chat.unread > 0 && (
                      <Badge className="bg-blue-600 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center p-0">
                        {chat.unread}
                      </Badge>
                    )}
                  </div>
                </div>
                <p className="text-sm text-gray-600 truncate">{chat.lastMessage}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const ChatArea = ({ selectedChat }) => {
  const [message, setMessage] = useState('');
  
  const messages = [
    {
      id: 1,
      sender: "John Doe",
      content: "Team meeting at 2 PM today",
      timestamp: "10:30 AM",
      avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=32&h=32&fit=crop&crop=face",
      isOwn: false
    },
    {
      id: 2,
      sender: "You",
      content: "Sounds good! I'll be there.",
      timestamp: "10:32 AM",
      avatar: null,
      isOwn: true
    },
    {
      id: 3,
      sender: "Sarah Smith",
      content: "Should I prepare the quarterly reports for the meeting?",
      timestamp: "10:35 AM",
      avatar: "https://images.unsplash.com/photo-1494790108755-2616b612c99e?w=32&h=32&fit=crop&crop=face",
      isOwn: false
    },
    {
      id: 4,
      sender: "You",
      content: "Yes, that would be great. Thanks Sarah!",
      timestamp: "10:36 AM",
      avatar: null,
      isOwn: true
    }
  ];

  const handleSendMessage = () => {
    if (message.trim()) {
      // Handle sending message
      setMessage('');
    }
  };

  if (!selectedChat) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <MessageCircle className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">Select a conversation</h3>
          <p className="text-gray-600">Choose a chat from the sidebar to start messaging</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col">
      {/* Chat Header */}
      <div className="p-4 border-b border-gray-200 bg-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Avatar className="h-10 w-10">
              {selectedChat.avatar ? (
                <AvatarImage src={selectedChat.avatar} />
              ) : (
                <AvatarFallback>
                  {selectedChat.type === 'channel' ? '#' : selectedChat.name.split(' ').map(n => n[0]).join('')}
                </AvatarFallback>
              )}
            </Avatar>
            <div>
              <h3 className="font-semibold text-gray-900">{selectedChat.name}</h3>
              <p className="text-sm text-gray-600">
                {selectedChat.type === 'channel' 
                  ? `${selectedChat.members} members`
                  : 'Active now'
                }
              </p>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm">
              <Bell className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
        {messages.map(msg => (
          <div key={msg.id} className={`flex items-start gap-3 ${msg.isOwn ? 'flex-row-reverse' : ''}`}>
            {!msg.isOwn && (
              <Avatar className="h-8 w-8">
                <AvatarImage src={msg.avatar} />
                <AvatarFallback>{msg.sender.split(' ').map(n => n[0]).join('')}</AvatarFallback>
              </Avatar>
            )}
            
            <div className={`max-w-xs lg:max-w-md ${msg.isOwn ? 'text-right' : ''}`}>
              {!msg.isOwn && (
                <p className="text-sm font-medium text-gray-900 mb-1">{msg.sender}</p>
              )}
              <div className={`rounded-lg p-3 ${
                msg.isOwn 
                  ? 'bg-blue-600 text-white' 
                  : 'bg-white text-gray-900 border border-gray-200'
              }`}>
                <p className="text-sm">{msg.content}</p>
              </div>
              <p className="text-xs text-gray-500 mt-1">{msg.timestamp}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Message Input */}
      <div className="p-4 border-t border-gray-200 bg-white">
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <Textarea
              placeholder="Type a message..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="resize-none"
              rows={1}
              onKeyPress={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSendMessage();
                }
              }}
            />
          </div>
          
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="sm">
              <Paperclip className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm">
              <Smile className="h-4 w-4" />
            </Button>
            <Button 
              onClick={handleSendMessage}
              disabled={!message.trim()}
              className="bg-blue-600 hover:bg-blue-700"
              size="sm"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export const ChatView = () => {
  const [selectedChat, setSelectedChat] = useState(null);

  return (
    <div className="h-[calc(100vh-8rem)] flex bg-white rounded-lg border border-gray-200 overflow-hidden">
      <ChatSidebar selectedChat={selectedChat} onSelectChat={setSelectedChat} />
      <ChatArea selectedChat={selectedChat} />
    </div>
  );
};

export default ChatView;