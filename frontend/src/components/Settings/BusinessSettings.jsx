import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Switch } from '../ui/switch';
import { Badge } from '../ui/badge';
import { 
  Building2, 
  Users, 
  Key, 
  Settings,
  Copy,
  Check,
  Save,
  RefreshCw
} from 'lucide-react';
import { useToast } from '../../hooks/use-toast';

export const BusinessSettings = ({ userRole }) => {
  const [business, setBusiness] = useState(null);
  const [formData, setFormData] = useState({});
  const [features, setFeatures] = useState({});
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    // Load business data from localStorage (will be replaced with API call)
    const businessData = JSON.parse(localStorage.getItem('currentBusiness') || '{}');
    setBusiness(businessData);
    setFormData({
      name: businessData.name || '',
      description: businessData.description || '',
      industry: businessData.industry || '',
      size: businessData.size || ''
    });
    setFeatures(businessData.features || {});
  }, []);

  const handleSaveBasicInfo = () => {
    if (userRole !== 'admin') {
      toast({
        title: "Access Denied",
        description: "Only admins can modify business settings",
        variant: "destructive"
      });
      return;
    }

    setLoading(true);
    
    // Simulate API call
    setTimeout(() => {
      const updatedBusiness = {
        ...business,
        ...formData,
        updatedAt: new Date().toISOString()
      };
      
      setBusiness(updatedBusiness);
      localStorage.setItem('currentBusiness', JSON.stringify(updatedBusiness));
      
      toast({
        title: "Settings Saved",
        description: "Business information has been updated successfully"
      });
      setLoading(false);
    }, 1000);
  };

  const handleFeatureToggle = (feature, enabled) => {
    if (userRole !== 'admin') {
      toast({
        title: "Access Denied",
        description: "Only admins can modify feature settings",
        variant: "destructive"
      });
      return;
    }

    const updatedFeatures = { ...features, [feature]: enabled };
    setFeatures(updatedFeatures);
    
    const updatedBusiness = {
      ...business,
      features: updatedFeatures,
      updatedAt: new Date().toISOString()
    };
    
    setBusiness(updatedBusiness);
    localStorage.setItem('currentBusiness', JSON.stringify(updatedBusiness));
    
    toast({
      title: `${enabled ? 'Enabled' : 'Disabled'}`,
      description: `${feature.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())} has been ${enabled ? 'enabled' : 'disabled'}`
    });
  };

  const generateNewInviteCode = () => {
    if (userRole !== 'admin') {
      toast({
        title: "Access Denied",
        description: "Only admins can generate invite codes",
        variant: "destructive"
      });
      return;
    }

    const newCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const updatedBusiness = {
      ...business,
      inviteCode: newCode,
      updatedAt: new Date().toISOString()
    };
    
    setBusiness(updatedBusiness);
    localStorage.setItem('currentBusiness', JSON.stringify(updatedBusiness));
    
    toast({
      title: "New Invite Code Generated",
      description: "Previous invite codes are now invalid"
    });
  };

  const copyInviteCode = () => {
    navigator.clipboard.writeText(business?.inviteCode || '');
    setCopied(true);
    toast({
      title: "Copied!",
      description: "Invite code copied to clipboard"
    });
    setTimeout(() => setCopied(false), 2000);
  };

  const featuresList = [
    { key: 'timeTracking', label: 'Time Tracking', description: 'Allow employees to clock in/out and track hours' },
    { key: 'scheduling', label: 'Scheduling', description: 'Create and manage employee schedules' },
    { key: 'tasks', label: 'Task Management', description: 'Assign and track tasks for team members' },
    { key: 'chat', label: 'Team Chat', description: 'Internal messaging and communication tools' },
    { key: 'reports', label: 'Advanced Reports', description: 'Generate detailed analytics and reports' },
    { key: 'documents', label: 'Document Management', description: 'Share and manage company documents' }
  ];

  if (!business?.name) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-gray-500">Loading business settings...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Business Settings</h1>
          <p className="text-gray-600">Manage your business configuration and features</p>
        </div>
        <Badge variant={userRole === 'admin' ? 'default' : 'secondary'}>
          {userRole === 'admin' ? 'Admin Access' : 'Employee View'}
        </Badge>
      </div>

      {/* Basic Information */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5" />
            Business Information
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="businessName">Business Name</Label>
              <Input
                id="businessName"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                disabled={userRole !== 'admin'}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="industry">Industry</Label>
              <select
                id="industry"
                value={formData.industry}
                onChange={(e) => setFormData(prev => ({ ...prev, industry: e.target.value }))}
                disabled={userRole !== 'admin'}
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50"
              >
                <option value="">Select Industry</option>
                <option value="retail">Retail</option>
                <option value="hospitality">Hospitality</option>
                <option value="healthcare">Healthcare</option>
                <option value="construction">Construction</option>
                <option value="technology">Technology</option>
                <option value="manufacturing">Manufacturing</option>
                <option value="other">Other</option>
              </select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              disabled={userRole !== 'admin'}
              rows={3}
            />
          </div>

          {userRole === 'admin' && (
            <div className="pt-2">
              <Button 
                onClick={handleSaveBasicInfo} 
                disabled={loading}
                className="bg-blue-600 hover:bg-blue-700"
              >
                {loading ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="h-4 w-4 mr-2" />
                    Save Changes
                  </>
                )}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Invite Code Management */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Key className="h-5 w-5" />
            Team Invite Code
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <code className="bg-gray-100 px-4 py-3 rounded-lg text-lg font-mono tracking-widest text-center block">
                {business.inviteCode}
              </code>
              <p className="text-sm text-gray-600 mt-2">
                Share this code with new team members to join your business
              </p>
            </div>
            
            <div className="flex flex-col gap-2">
              <Button
                variant="outline"
                onClick={copyInviteCode}
                className="w-full"
              >
                {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
              </Button>
              
              {userRole === 'admin' && (
                <Button
                  variant="outline"
                  onClick={generateNewInviteCode}
                  className="w-full"
                >
                  <RefreshCw className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Feature Management */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Settings className="h-5 w-5" />
            Feature Settings
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {featuresList.map(feature => (
              <div key={feature.key} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0">
                <div className="flex-1">
                  <h3 className="font-medium text-gray-900">{feature.label}</h3>
                  <p className="text-sm text-gray-600">{feature.description}</p>
                </div>
                
                <Switch
                  checked={features[feature.key] || false}
                  onCheckedChange={(checked) => handleFeatureToggle(feature.key, checked)}
                  disabled={userRole !== 'admin'}
                />
              </div>
            ))}
          </div>
          
          {userRole !== 'admin' && (
            <div className="mt-4 p-3 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600">
                <Users className="h-4 w-4 inline mr-1" />
                Feature settings can only be modified by business administrators
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default BusinessSettings;