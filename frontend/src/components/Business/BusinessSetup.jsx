import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../ui/dialog';
import { Building2, Users, Key, Copy, Check } from 'lucide-react';
import { useToast } from '../../hooks/use-toast';

export const BusinessSetup = ({ isOpen, onClose, onComplete }) => {
  const [step, setStep] = useState(1);
  const [businessData, setBusinessData] = useState({
    name: '',
    description: '',
    industry: '',
    size: ''
  });
  const [inviteCode, setInviteCode] = useState('');
  const [copied, setCopied] = useState(false);
  const { toast } = useToast();

  const generateInviteCode = () => {
    const code = Math.random().toString(36).substring(2, 8).toUpperCase();
    setInviteCode(code);
  };

  const handleCreateBusiness = () => {
    if (!businessData.name.trim()) {
      toast({
        title: "Error",
        description: "Business name is required",
        variant: "destructive"
      });
      return;
    }
    
    generateInviteCode();
    setStep(2);
  };

  const handleComplete = () => {
    // Store business data in localStorage (will be replaced with API call)
    const business = {
      ...businessData,
      inviteCode,
      adminId: 'current-user-id',
      createdAt: new Date().toISOString(),
      features: {
        timeTracking: true,
        scheduling: true,
        tasks: true,
        chat: true,
        reports: true,
        documents: true
      }
    };
    
    localStorage.setItem('currentBusiness', JSON.stringify(business));
    localStorage.setItem('userRole', 'admin');
    
    onComplete(business);
    onClose();
  };

  const copyInviteCode = () => {
    navigator.clipboard.writeText(inviteCode);
    setCopied(true);
    toast({
      title: "Copied!",
      description: "Invite code copied to clipboard"
    });
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-blue-600" />
            Create Your Business
          </DialogTitle>
        </DialogHeader>

        {step === 1 && (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="businessName">Business Name *</Label>
              <Input
                id="businessName"
                placeholder="Enter your business name"
                value={businessData.name}
                onChange={(e) => setBusinessData(prev => ({ ...prev, name: e.target.value }))}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Brief description of your business"
                value={businessData.description}
                onChange={(e) => setBusinessData(prev => ({ ...prev, description: e.target.value }))}
                rows={3}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="industry">Industry</Label>
                <select
                  id="industry"
                  value={businessData.industry}
                  onChange={(e) => setBusinessData(prev => ({ ...prev, industry: e.target.value }))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
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

              <div className="space-y-2">
                <Label htmlFor="size">Team Size</Label>
                <select
                  id="size"
                  value={businessData.size}
                  onChange={(e) => setBusinessData(prev => ({ ...prev, size: e.target.value }))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select Size</option>
                  <option value="1-10">1-10 employees</option>
                  <option value="11-50">11-50 employees</option>
                  <option value="51-200">51-200 employees</option>
                  <option value="200+">200+ employees</option>
                </select>
              </div>
            </div>

            <div className="flex gap-2 pt-4">
              <Button variant="outline" onClick={onClose} className="flex-1">
                Cancel
              </Button>
              <Button onClick={handleCreateBusiness} className="flex-1 bg-blue-600 hover:bg-blue-700">
                Create Business
              </Button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto">
              <Check className="h-8 w-8 text-green-600" />
            </div>
            
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Business Created Successfully!
              </h3>
              <p className="text-gray-600 mb-4">
                Your business "<strong>{businessData.name}</strong>" has been created.
              </p>
            </div>

            <Card className="bg-blue-50 border-blue-200">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <Key className="h-4 w-4 text-blue-600" />
                  <span className="text-sm font-medium text-blue-900">Your Invite Code</span>
                </div>
                
                <div className="flex items-center gap-2">
                  <code className="bg-white px-3 py-2 rounded border text-lg font-mono tracking-widest text-blue-600 flex-1 text-center">
                    {inviteCode}
                  </code>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={copyInviteCode}
                    className="border-blue-300 text-blue-600 hover:bg-blue-100"
                  >
                    {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                  </Button>
                </div>
                
                <p className="text-xs text-blue-700 mt-2">
                  Share this code with your team members to join your business
                </p>
              </CardContent>
            </Card>

            <Button onClick={handleComplete} className="w-full bg-blue-600 hover:bg-blue-700">
              <Users className="h-4 w-4 mr-2" />
              Start Managing Your Team
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default BusinessSetup;