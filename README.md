Android Application for YHack (Yale's Hackathon).

Many colleges have an annual game of Humans vs Zombies, where all participants, excluding a few,
start off as humans. The zombies catch and "infect" the humans who then become zombies and try to 
catch and "infect" the other humans.

Another popular game on college campuses is Assasins. Each person playing has a person they are supposed to "kill".
No one knows who is trying to kill them. As players advance by "killing" more people without dying, more people 
are assigned to kill that person in order to keep the game fair and fun.

Snapshot is Assasins as a mobile app with QRCodes and pictures. Players see the FB Profile Picture of the people
who they are trying to kill and are tasked with scanning the QRCode on that person's body. Based on gyrometer movements,
points will be awarded for the kill (ie. fewer gyrometer movements indicates less movement which indicates better stealth,
which earns more points).

An algorithm running on Microsoft's Azure DB is responsible for delegating an assasin's target and rearranging targets
to keep the game interesting.
