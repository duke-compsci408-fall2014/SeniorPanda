Team Name: Senior Panda
Team Members: Justin (Zihao) Zhang, Sanmay Jain, Steve (Siyang) Wang

Project Plan Draft

Overall Goals and Scope

The project will deliver an Android tablet app. This app aims to facilitate communication among senior patients and doctors. The app targets senior citizen patients impacted by Parkinson’s disease. The app helps senior patients record their health information, reminds them to take medicine as their doctors ask at appropriate times and informs their doctors consistently. Moreover, this app attracts seniors patients by displaying family photos and weather information continuously throughout the day so that the patients will look at the apps frequently. It also makes it easier for doctors to view their patients’ health information easily and drive medicine decisions accordingly. Overall, Senior Panda strives to be the go-to app for seniors users by providing cleaner and easier-to-access interface and encapsulates unnecessarily complicated and cluttered Android functionalities. 

The primary usability goals would be simplicity and “hands off” orientation. We expect users to put their devices as kitchen countertop installation. We will primarily focus on two areas. The first one is consumer content delivery. We will incorporate family photo streaming service, weather and news daily updates, and possibly other forms of entertainment for seniors. By doing so, we encourage seniors to use the app consistently. The second one is medical query delivery and response capture to populate a server side database related to medication and symptom information. 

The project will help our client, Bamboo Mobile Health, to improve its existing app by adding more functionalities and expediting data transmission. We also want to use this project as a great opportunity to learn about Android programming in detail. We want to gain a deeper understanding about how the data flow is processed between the front end and the cloud database system. 

Design Goals

The overall project is designed to prioritize the following functionalities (sorted according to the decreasing priority):
User login system, and the database that supports the database scheme design that facilitate easy registration, and pulling of user information from doctor’s end
Integrated notification system that pops up in a more obvious and friendly way so to 
remind user to input health condition data, and
ask user to take medication according to the schedule, and 
notify the user to set up appointment with doctor when it detects the users’ health condition has deteriorated. 
Integrated Data-visualization functionality that helps doctor to gain better understanding of the user’s condition by showing
	color coded calendar view to denote the wellness, and 
	pie chart to show percentage of days with certain symptoms
Simplified photo flipping functionality that is connected to the cloud shared folder service like Dropbox, Google Drive, or Skydrive 
that automatically shows the pushed photo by other family member, and
occupy the tablet’s main screen when idle to allow, to shield user from clutterous and not-so-user-friendly Android environment

Alternatively, following are the points that we consider is secondary, and therefore we consider implementing and actualizing them if given more time and resources:

Doctor’s UI that allows doctor to input patient ID and pull patient’s health and medical records represented graphically. We put this as secondary, because we have quite a lot of functionalities already listed above, and we hope to focus those essential functionalities first before we work on this extra feature
How the UI looks, and the visuals and graphics is secondary, as the UI can be modified by the XML interface builder, and the graphics can be modified later by replacing the pictures and graph address

With these functionalities in mind, the design of the overall project is like below:
	Module:
		Database interaction and login/user profile management
		User UIs: UI for patient, and UI for doctors
		Data visualization
		Notification System 

The modules mentioned above would all require programming skills, and therefore we expect client to do least amount of modification by him/herself. We will also supply detailed technical document / project artifact, and extensive README for later modification if necessary. Client, on the other hand, is responsible for updating the visuals, and updating the text/ wording inside each user interface, and the visualization. 

Dependencies from the client:
	Database: Amazon AWS/EC2, and DreamFactory wrapper
	Device: Android Tablet
	Source code for the project that we build upon (from the previous release)
	Visuals and graphics that include app icon, the button, and color scheme of UI
	

Concerns. Any concerns or risks the client should know. (Sanmay)
Our team does not have any major concerns. We would like to access pre-existing code and the database as soon as possible to familiarize ourselves with the technical details. In addition, we do have concerns about the structure and visualization of the app. Since the main goal of this project is to create a user friendly app, we would like to focus on making the app visually pleasing and functionally simple.

Team Organization

Sanmay is our Business Analyst and Quality Tester. He will be our main point of contact between our team and the client. Regarding distribution of the work involved, Sanmay is responsible for improving existing notifications. Steve, our Technical Lead, is responsible for photo slideshow feature and the database system. Justin, our Product Manager, is responsible for data visualization feature. He will also help Steve with the back end communication. 


Task Deliverables for each sprint

Sprint 1 (pretotype): Simple mockup UI design.  
Sprint 2 (prototype): Finish GUI design of core features.
Sprint 3 (baseline): Integrate backend database into our app. Display dynamically in database instead of using hard-coded data.
Sprint 4 (alpha): Improve app from doctor’s point of view. This would include implementation of notifications and data visualization.
Sprint 5 (beta): Start distributing app to test clients for user testing.
Sprint 6 (Release): Take client considerations into account and fix any final bugs.

 




















