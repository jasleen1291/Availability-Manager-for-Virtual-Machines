Availability-Manager-for-Virtual-Machines
=========================================

The goal of this project is to create an availability manager to monitor the status of Virtual Machines on a datacenter in a Vsphere server using Java and Vmware web services SDK. The project monitors both virtual machines and hosts for failure. When a failure is detected first the host is checked for failure. If the host has failed, an attempt to reconnect is done. If the reconnection fails, another host is searched for migrating virtual machines. If no other host is present, a new host is added and vm is restarted on that host. If virtual machine fails, it is reverted to an old cached image. The caching of images is done every 10 minutes. All the task mentioned are automated.
Objectives

•	Gain experience with several hypervisors and the corresponding management servers 
•	Explore the capabilities offered by these components and their APIs, 
•	Apply the concepts discussed in the course to a real-world problem, and 
•	Learn about the issues arising from interoperability 

Needs
•	Manual monitoring of virtual machines is a difficult, time consuming and resource consuming.
•	Manual fault detection is hard especially in environments with large number of virtual machines.
•	In a scenario with large number of virtual machines and hosts manually caching images of each virtual machine on a regular interval becomes hard, inefficient and unreliable.
•	Availability manager is not only needed to automate monitoring, fault detection, caching tasks but also required for lower downtime of machines.
