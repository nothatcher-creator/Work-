import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../ui/dialog';
import { UserPlus, Building2, AlertCircle, Check } from 'lucide-react';
import { useToast } from '../../hooks/use-toast';

export const JoinBusiness = ({ isOpen, onClose, onJoin }) => {
  const [inviteCode, setInviteCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [businessInfo, setBusinessInfo] = useState(null);
  const { toast } = useToast();

  // Mock businesses for demo (will be replaced with API call)
  const mockBusinesses = {
    'ABC123': {
      name: 'Tech Solutions Inc',
      description: 'Leading technology consulting firm',
      industry: 'Technology',
      memberCount: 45
    },
    'XYZ789': {
      name: 'Green Valley Restaurant',
      description: 'Farm-to-table dining experience',
      industry: 'Hospitality',
      memberCount: 23
    }
  };

  const handleVerifyCode = async () => {
    if (!inviteCode.trim()) {
      toast({
        title: "Error",
        description: "Please enter an invite code",
        variant: "destructive"
      });
      return;
    }

    setLoading(true);
    
    // Simulate API call
    setTimeout(() => {
      const business = mockBusinesses[inviteCode.toUpperCase()];
      
      if (business) {
        setBusinessInfo(business);
        toast({
          title: "Code Verified!",
          description: "Business found. Click join to continue."
        });
      } else {
        toast({
          title: "Invalid Code",
          description: "The invite code you entered is not valid or has expired.",
          variant: "destructive"
        });
      }
      setLoading(false);
    }, 1500);
  };

  const handleJoinBusiness = () => {
    if (businessInfo) {
      // Store business data (will be replaced with API call)
      const businessData = {
        ...businessInfo,
        inviteCode: inviteCode.toUpperCase(),
        joinedAt: new Date().toISOString(),
        features: {
          timeTracking: true,
          scheduling: true,
          tasks: true,
          chat: true,
          reports: false, // Limited access for employees
          documents: true
        }
      };
      
      localStorage.setItem('currentBusiness', JSON.stringify(businessData));
      localStorage.setItem('userRole', 'employee');
      
      onJoin(businessData);
      onClose();
    }
  };

  const handleReset = () => {
    setBusinessInfo(null);
    setInviteCode('');
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UserPlus className="h-5 w-5 text-blue-600" />
            Join a Business
          </DialogTitle>
        </DialogHeader>

        {!businessInfo ? (
          <div className="space-y-4">
            <div className="text-center py-4">
              <Building2 className="h-12 w-12 text-gray-400 mx-auto mb-3" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                Enter Invite Code
              </h3>
              <p className="text-gray-600 text-sm">
                Ask your manager for the business invite code to join your team
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="inviteCode">Invite Code</Label>
              <Input
                id="inviteCode"
                placeholder="Enter 6-character code"
                value={inviteCode}
                onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                maxLength={6}
                className="text-center text-lg font-mono tracking-widest"
              />
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <div className="flex items-start gap-2">
                <AlertCircle className="h-4 w-4 text-blue-600 mt-0.5 flex-shrink-0" />
                <div className="text-sm text-blue-800">
                  <p className="font-medium">Need help?</p>
                  <p>Contact your manager or HR department for the invite code.</p>
                </div>
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              <Button variant="outline" onClick={onClose} className="flex-1">
                Cancel
              </Button>
              <Button 
                onClick={handleVerifyCode} 
                disabled={loading || inviteCode.length !== 6}
                className="flex-1 bg-blue-600 hover:bg-blue-700"
              >
                {loading ? 'Verifying...' : 'Verify Code'}
              </Button>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="text-center py-4">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <Check className="h-8 w-8 text-green-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Business Found!
              </h3>
            </div>

            <Card className="border-green-200 bg-green-50">
              <CardContent className="p-4">
                <h3 className="font-semibold text-gray-900 mb-2">{businessInfo.name}</h3>
                <p className="text-sm text-gray-600 mb-3">{businessInfo.description}</p>
                
                <div className="flex justify-between text-sm">
                  <div>
                    <span className="text-gray-500">Industry:</span>
                    <span className="ml-1 font-medium">{businessInfo.industry}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">Members:</span>
                    <span className="ml-1 font-medium">{businessInfo.memberCount}</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            <div className="flex gap-2 pt-2">
              <Button variant="outline" onClick={handleReset} className="flex-1">
                Try Different Code
              </Button>
              <Button onClick={handleJoinBusiness} className="flex-1 bg-green-600 hover:bg-green-700">
                Join Business
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default JoinBusiness;