import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../ui/dialog';
import { Building2, UserPlus, ArrowRight } from 'lucide-react';
import BusinessSetup from '../Business/BusinessSetup';
import JoinBusiness from '../Business/JoinBusiness';

export const InitialSetup = ({ isOpen, onComplete }) => {
  const [view, setView] = useState('choice'); // 'choice', 'create', 'join'
  const [showBusinessSetup, setShowBusinessSetup] = useState(false);
  const [showJoinBusiness, setShowJoinBusiness] = useState(false);

  const handleCreateBusiness = () => {
    setShowBusinessSetup(true);
    setView('create');
  };

  const handleJoinBusiness = () => {
    setShowJoinBusiness(true);
    setView('join');
  };

  const handleBusinessSetupComplete = (businessData) => {
    onComplete(businessData, 'admin');
  };

  const handleJoinComplete = (businessData) => {
    onComplete(businessData, 'employee');
  };

  const handleBack = () => {
    setView('choice');
    setShowBusinessSetup(false);
    setShowJoinBusiness(false);
  };

  return (
    <>
      <Dialog open={isOpen && view === 'choice'} onOpenChange={() => {}}>
        <DialogContent className="max-w-lg" hideClose>
          <DialogHeader>
            <DialogTitle className="text-center text-xl">
              Welcome to Connecteam
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-6 py-4">
            <div className="text-center">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Building2 className="h-8 w-8 text-blue-600" />
              </div>
              <h2 className="text-lg font-semibold text-gray-900 mb-2">
                Get Started with Your Team
              </h2>
              <p className="text-gray-600">
                Choose how you'd like to set up your workspace
              </p>
            </div>

            <div className="grid gap-4">
              <Card 
                className="cursor-pointer border-2 hover:border-blue-500 hover:shadow-md transition-all"
                onClick={handleCreateBusiness}
              >
                <CardContent className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                      <Building2 className="h-6 w-6 text-blue-600" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-gray-900 mb-1">
                        Create New Business
                      </h3>
                      <p className="text-sm text-gray-600">
                        Set up a new business account and invite your team members
                      </p>
                    </div>
                    <ArrowRight className="h-5 w-5 text-gray-400" />
                  </div>
                </CardContent>
              </Card>

              <Card 
                className="cursor-pointer border-2 hover:border-green-500 hover:shadow-md transition-all"
                onClick={handleJoinBusiness}
              >
                <CardContent className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                      <UserPlus className="h-6 w-6 text-green-600" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-gray-900 mb-1">
                        Join Existing Business
                      </h3>
                      <p className="text-sm text-gray-600">
                        Enter an invite code to join your team's workspace
                      </p>
                    </div>
                    <ArrowRight className="h-5 w-5 text-gray-400" />
                  </div>
                </CardContent>
              </Card>
            </div>

            <div className="text-center">
              <p className="text-xs text-gray-500">
                You can switch between businesses later in your account settings
              </p>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <BusinessSetup
        isOpen={showBusinessSetup}
        onClose={handleBack}
        onComplete={handleBusinessSetupComplete}
      />

      <JoinBusiness
        isOpen={showJoinBusiness}
        onClose={handleBack}
        onJoin={handleJoinComplete}
      />
    </>
  );
};

export default InitialSetup;