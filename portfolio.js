import React, { useState, useEffect } from 'react';
import { Github, Linkedin, Mail, Phone, MapPin, ExternalLink, Code, Briefcase, GraduationCap, Award, ChevronDown, Menu, X, Sparkles, Zap, Rocket, TrendingUp, Compass } from 'lucide-react';

export default function Portfolio() {
  const [activeSection, setActiveSection] = useState('home');
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const [particles, setParticles] = useState([]);

  // Setup Intersection Observer for professional scroll highlighting
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setActiveSection(entry.target.id);
          }
        });
      },
      { rootMargin: '-30% 0px -30% 0px' } // Highlight when section is centered
    );

    ['home', 'about', 'experience', 'projects', 'activities', 'skills', 'contact'].forEach((id) => {
      const element = document.getElementById(id);
      if (element) observer.observe(element);
    });

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    const handleMouseMove = (e) => {
      setMousePosition({ x: e.clientX, y: e.clientY });
    };
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  useEffect(() => {
    // Fewer, larger, and slower particles for a more sophisticated effect
    const newParticles = Array.from({ length: 20 }, (_, i) => ({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 5 + 3,
      duration: Math.random() * 30 + 20
    }));
    setParticles(newParticles);
  }, []);

  const skills = {
    languages: ['Python', 'Java', 'C', 'C++', 'JavaScript'],
    frontend: ['React.js', 'HTML5', 'CSS3', 'Tailwind CSS'],
    backend: ['Node.js', 'Express.js', 'Flask', 'PHP'],
    databases: ['MySQL', 'MongoDB', 'Firebase'],
    aiml: ['TensorFlow', 'OpenCV', 'Mediapipe', 'OpenAI APIs'],
    tools: ['Git', 'Docker', 'Selenium', 'RESTful APIs', 'AWS']
  };

  const projects = [
    {
      title: 'AI Shopping Jarvis',
      description: 'An AI-powered shopping assistant utilizing voice commands for intelligent search and recommendation of top-rated, low-cost products across multiple e-commerce platforms.',
      tech: ['Flask', 'Selenium', 'Python', 'OpenAI', 'Hugging Face'],
      highlights: ['Voice command recognition', 'Smart product ranking algorithm', 'Real-time data scraping'],
      gradient: 'from-purple-500 to-pink-500',
      icon: Sparkles
    },
    {
      title: 'Driver Drowsiness Detection System',
      description: 'A robust, real-time alert system designed to enhance road safety by accurately detecting driver fatigue through continuous facial landmark analysis.',
      tech: ['Python', 'Mediapipe', 'OpenCV', 'Computer Vision'],
      highlights: ['High-performance real-time processing', 'Drowsiness algorithm implementation', 'Public safety application focus'],
      gradient: 'from-blue-500 to-cyan-500',
      icon: Zap
    },
    {
      title: 'Real-Time Transport Enquiry System',
      description: 'A comprehensive web-based platform providing commuters with real-time transport information, including seat availability, route details, and live schedules for trains and buses.',
      tech: ['Python', 'Flask', 'REST API', 'MySQL', 'HTML', 'CSS'],
      highlights: ['Integrated live transport data', 'Efficient query handling and display', 'Scalable multi-modal design'],
      gradient: 'from-green-500 to-emerald-500',
      icon: Rocket
    },
    {
      title: 'Senior Support Group Management Platform',
      description: 'A responsive and secure platform connecting senior citizens with dedicated caregivers and local support groups, featuring robust authentication and data management.',
      tech: ['PHP', 'HTML', 'CSS', 'MySQL'],
      highlights: ['Secure user authentication', 'Community data management', 'Direct social impact solution'],
      gradient: 'from-orange-500 to-red-500',
      icon: Compass // Changed from Star for a slightly more professional icon
    }
  ];

  const experience = [
    {
      company: 'Infosys Springboard',
      role: 'AI & Machine Learning Intern',
      period: 'Oct 2025 – Present',
      location: 'Remote, India',
      points: [
        'Designed and implemented **predictive models** using supervised and unsupervised learning techniques.',
        'Gained practical expertise in the complete AI lifecycle: model training, evaluation, and deployment.',
        'Applied industry-standard practices to AI applications, focusing on scalability and performance.'
      ]
    },
    {
      company: 'VaultofCodes',
      role: 'AI & Prompt Engineering Intern',
      period: 'Aug 2025 – Sep 2025',
      location: 'Remote, India',
      points: [
        'Significantly enhanced **LLM response accuracy by 35%** through rigorous prompt engineering and testing methodologies.',
        'Developed and integrated AI-based utility tools using **Python and OpenAI APIs** for internal projects.',
        'Collaborated effectively on the deployment pipeline of various AI-driven applications.'
      ]
    }
  ];

  const activities = [
    {
        title: 'Technical Events Coordinator',
        organization: 'RYMEC College',
        period: 'October 2025',
        points: [
            'Organized and managed **10+ Tech events and coding bootcamps**, successfully engaging over **100+ participants**.',
            'Spearheaded a volunteer team to ensure seamless planning, logistics, and execution of campus technical events.'
        ],
        icon: Briefcase,
        color: 'blue'
    },
    {
        title: 'Major Hackathon Participant',
        organization: 'Smart India Hackathon & State-Level',
        period: 'Jan 2024 & 2025',
        points: [
            'Competed in **Smart India Hackathon 2024**, developing an innovative AI-ML solution for a predefined societal problem.',
            'Actively participated in the State-Level Hackathon by RYMEC, demonstrating rapid prototyping and problem-solving skills.'
        ],
        icon: Rocket,
        color: 'purple'
    }
  ];

  const scrollToSection = (sectionId) => {
    setActiveSection(sectionId);
    setIsMenuOpen(false);
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const navItems = ['home', 'about', 'experience', 'projects', 'skills', 'activities', 'contact'];

  // Refined Component Return
  return (
    <div className="min-h-screen bg-slate-950 text-white overflow-hidden font-sans">
      {/* Animated Background Particles (Subtle) */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden opacity-30">
        {particles.map((particle) => (
          <div
            key={particle.id}
            className="absolute rounded-full bg-blue-500/50"
            style={{
              left: `${particle.x}%`,
              top: `${particle.y}%`,
              width: `${particle.size}px`,
              height: `${particle.size}px`,
              animation: `float ${particle.duration}s infinite ease-in-out`
            }}
          />
        ))}
      </div>

      {/* Cursor Glow Effect (Refined Blur/Opacity) */}
      <div 
        className="fixed w-96 h-96 rounded-full pointer-events-none blur-3xl opacity-10 bg-gradient-to-r from-blue-500 to-purple-500 transition-all duration-300 z-0"
        style={{
          left: mousePosition.x - 192,
          top: mousePosition.y - 192,
        }}
      />

      <style jsx>{`
        @keyframes float {
          0%, 100% { transform: translateY(0) translateX(0); }
          50% { transform: translateY(-30px) translateX(20px); }
        }
        @keyframes slideInUp {
          from { transform: translateY(20px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
        @keyframes scaleIn {
          from { transform: scale(0.95); opacity: 0; }
          to { transform: scale(1); opacity: 1; }
        }
        .animate-slide-in-up { animation: slideInUp 0.8s ease-out forwards; }
        .animate-scale-in { animation: scaleIn 0.6s ease-out forwards; }
        .bg-glow-hover:hover {
          box-shadow: 0 0 20px rgba(59, 130, 246, 0.4);
        }
      `}</style>

      {/* Navigation */}
      <nav className={`sticky top-0 w-full z-50 transition-all duration-300 ${scrolled ? 'bg-slate-950/95 backdrop-blur-lg shadow-xl shadow-blue-500/10' : 'bg-transparent'}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            {/* Logo/Initials: More polished, less blinking */}
            <div className="text-2xl font-extrabold tracking-widest bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent hover:scale-105 transition-transform duration-300">
              HK.DEV
            </div>
            
            <div className="hidden md:flex space-x-8">
              {navItems.map((item) => (
                <button
                  key={item}
                  onClick={() => scrollToSection(item)}
                  className={`capitalize font-medium text-sm tracking-wider pb-1 transition-all duration-300 relative group ${
                    activeSection === item ? 'text-blue-400 font-semibold' : 'text-gray-300 hover:text-blue-300'
                  }`}
                >
                  {item}
                  <span className={`absolute bottom-0 left-0 w-full h-0.5 bg-blue-400 transition-all duration-300 ${activeSection === item ? 'scale-x-100' : 'scale-x-0 group-hover:scale-x-50'}`}></span>
                </button>
              ))}
            </div>

            <button 
              className="md:hidden text-gray-300 hover:text-blue-400 transition-colors duration-300"
              onClick={() => setIsMenuOpen(!isMenuOpen)}
            >
              {isMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {isMenuOpen && (
          <div className="md:hidden bg-slate-900/95 backdrop-blur-lg">
            <div className="px-4 py-4 space-y-3">
              {navItems.map((item, index) => (
                <button
                  key={item}
                  onClick={() => scrollToSection(item)}
                  className="block w-full text-left capitalize font-medium text-gray-300 hover:text-blue-400 transition-all duration-300 py-2 border-b border-slate-800 last:border-b-0"
                >
                  {item}
                </button>
              ))}
            </div>
          </div>
        )}
      </nav>

      <main className="relative z-10">
        {/* Hero Section */}
        <section id="home" className="min-h-screen flex items-center justify-center px-4 pt-24 pb-12">
          <div className="max-w-4xl mx-auto text-center animate-scale-in" style={{ animationDelay: '0.1s' }}>
            <p className="text-lg sm:text-xl text-blue-400 font-medium mb-3 tracking-widest uppercase">
              Hello, I'm
            </p>
            <h1 className="text-6xl sm:text-8xl font-extrabold mb-4 leading-tight">
              <span className="bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
                Kuruba Hemanth Kishore
              </span>
            </h1>
            <p className="text-2xl sm:text-3xl text-gray-300 font-light mb-6 animate-slide-in-up" style={{ animationDelay: '0.3s' }}>
              Full Stack Developer | AI/ML Enthusiast
            </p>
            <p className="text-lg text-gray-400 mb-10 max-w-2xl mx-auto animate-slide-in-up" style={{ animationDelay: '0.5s' }}>
              Passionate about creating innovative, scalable software solutions with expertise in web technologies and intelligent systems.
            </p>
            <div className="flex flex-wrap justify-center gap-5 animate-slide-in-up" style={{ animationDelay: '0.7s' }}>
              <a href="mailto:hemanthk.cse.rymec@gmail.com" className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 px-7 py-3 rounded-lg font-semibold transition-all duration-300 transform hover:scale-105 shadow-xl shadow-blue-500/20">
                <Mail className="w-5 h-5" />
                Email Me
              </a>
              <a href="https://www.linkedin.com/in/kuruba-hemanth-kishore/" target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 border border-gray-700 hover:border-blue-500 px-7 py-3 rounded-lg font-semibold text-gray-300 hover:text-blue-400 transition-all duration-300 transform hover:scale-105">
                <Linkedin className="w-5 h-5" />
                View Profile
              </a>
            </div>
            <button onClick={() => scrollToSection('about')} className="mt-12 group">
              <ChevronDown className="w-8 h-8 text-blue-400 group-hover:translate-y-1 transition-transform duration-300" />
            </button>
          </div>
        </section>

        {/* About Section */}
        <section id="about" className="py-20 px-4">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold mb-12 text-center text-gray-200">
              <span className="border-b-4 border-blue-500 pb-1">About Me</span>
            </h2>
            <div className="grid md:grid-cols-3 gap-8">
              <div className="md:col-span-2 bg-slate-900/50 backdrop-blur-sm rounded-xl p-8 border border-slate-800 hover:border-blue-600 transition-all duration-300 hover:shadow-2xl hover:shadow-blue-900/30 animate-slide-in-up">
                <h3 className="text-3xl font-bold mb-4 text-white flex items-center gap-3">
                  <Sparkles className="w-6 h-6 text-purple-400" />
                  Professional Summary
                </h3>
                <p className="text-gray-300 leading-relaxed text-lg">
                  A results-driven **Software Developer** and **AI/ML Enthusiast** specializing in building scalable web applications, robust APIs, and comprehensive full-stack solutions. I possess a strong foundation in modern frameworks like **React.js** and **Flask**, complemented by practical experience in implementing intelligent systems using **OpenAI** and **Computer Vision** libraries. Committed to continuous learning and delivering innovative technological solutions that achieve measurable impact.
                </p>
              </div>

              <div className="bg-slate-900/50 backdrop-blur-sm rounded-xl p-8 border border-slate-800 hover:border-purple-600 transition-all duration-300 hover:shadow-2xl hover:shadow-purple-900/30 animate-slide-in-up" style={{ animationDelay: '0.2s' }}>
                <GraduationCap className="w-10 h-10 text-blue-400 mb-4" />
                <h3 className="text-2xl font-bold mb-3 text-white">Education</h3>
                <p className="text-gray-300 mb-1">
                  <strong className="text-white">B.Tech in Computer Science & Eng.</strong>
                </p>
                <p className="text-blue-400 mb-1">RYMEC, Ballari</p>
                <p className="text-gray-400 mb-1">Expected: May 2026</p>
                <p className="text-lg font-semibold mt-2 text-green-400">81.48% (Cumulative)</p>
              </div>
            </div>

            <div className="mt-8 bg-slate-900/50 backdrop-blur-sm rounded-xl p-6 border border-slate-800 hover:border-gray-600 transition-all duration-300 hover:shadow-2xl hover:shadow-gray-900/30 animate-slide-in-up" style={{ animationDelay: '0.4s' }}>
                <div className="grid md:grid-cols-3 gap-6 text-center">
                    <div className="py-2">
                        <MapPin className="w-6 h-6 text-red-400 mx-auto mb-2" />
                        <p className="text-sm text-gray-400">Location</p>
                        <p className="text-white font-medium">Bellary, India</p>
                    </div>
                    <div className="py-2 border-x border-slate-800">
                        <Mail className="w-6 h-6 text-blue-400 mx-auto mb-2" />
                        <p className="text-sm text-gray-400">Email</p>
                        <a href="mailto:hemanthk.cse.rymec@gmail.com" className="text-blue-400 hover:underline text-sm">hemanthk.cse.rymec@gmail.com</a>
                    </div>
                    <div className="py-2">
                        <Phone className="w-6 h-6 text-green-400 mx-auto mb-2" />
                        <p className="text-sm text-gray-400">Phone</p>
                        <a href="tel:+917204948579" className="text-blue-400 hover:underline text-sm">+91 7204948579</a>
                    </div>
                </div>
            </div>
          </div>
        </section>

        {/* --- */}

        {/* Experience Section */}
        <section id="experience" className="py-20 px-4 bg-slate-900/70">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold mb-12 text-center text-gray-200">
              <span className="border-b-4 border-blue-500 pb-1">Work Experience</span>
            </h2>
            <div className="space-y-10">
              {experience.map((exp, index) => (
                <div 
                  key={index} 
                  className="bg-slate-900/50 backdrop-blur-sm rounded-xl p-8 border-l-4 border-blue-500 shadow-xl hover:shadow-blue-500/20 transition-all duration-300 transform hover:translate-x-1 animate-slide-in-up"
                  style={{ animationDelay: `${index * 0.2}s` }}
                >
                  <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-4">
                    <div>
                      <h3 className="text-2xl font-bold text-white mb-1">{exp.role}</h3>
                      <p className="text-blue-400 text-lg font-medium flex items-center gap-2">
                        <TrendingUp className="w-5 h-5" />
                        {exp.company}
                      </p>
                    </div>
                    <div className="text-gray-400 mt-2 md:mt-0 md:text-right text-sm">
                      <p className="font-semibold">{exp.period}</p>
                      <p>{exp.location}</p>
                    </div>
                  </div>
                  <ul className="space-y-3 mt-4">
                    {exp.points.map((point, i) => (
                      <li key={i} className="flex items-start gap-2 text-gray-300 text-base">
                        <span className="text-blue-400 mt-1 flex-shrink-0">►</span>
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* --- */}

        {/* Projects Section */}
        <section id="projects" className="py-20 px-4">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold mb-12 text-center text-gray-200">
              <span className="border-b-4 border-blue-500 pb-1">Featured Projects</span>
            </h2>
            <div className="grid md:grid-cols-2 gap-8">
              {projects.map((project, index) => {
                const IconComponent = project.icon;
                return (
                  <div 
                    key={index} 
                    className="group bg-slate-900/50 backdrop-blur-sm rounded-xl p-7 border border-slate-800 transition-all duration-300 transform hover:scale-[1.02] hover:shadow-2xl hover:shadow-blue-500/20 animate-slide-in-up"
                    style={{ animationDelay: `${index * 0.1}s` }}
                  >
                    <div className="flex items-center gap-4 mb-4">
                      <IconComponent className="w-8 h-8 text-blue-400 group-hover:text-purple-400 transition-colors duration-300" />
                      <h3 className="text-2xl font-bold text-white group-hover:text-blue-300 transition-colors duration-300">{project.title}</h3>
                    </div>
                    <p className="text-gray-400 mb-5 leading-relaxed text-base">{project.description}</p>
                    <div className="mb-5">
                      <p className="font-semibold text-sm text-blue-300 mb-2">Key Highlights:</p>
                      <ul className="space-y-1">
                          {project.highlights.map((highlight, i) => (
                              <li key={i} className="flex items-center gap-2 text-gray-400 text-sm">
                                  <span className="text-green-400 flex-shrink-0">✓</span>
                                  {highlight}
                              </li>
                          ))}
                      </ul>
                    </div>
                    <div className="flex flex-wrap gap-2 pt-3 border-t border-slate-800">
                      {project.tech.map((tech, i) => (
                        <span 
                          key={i} 
                          className="px-3 py-1 bg-blue-900/30 rounded-full text-xs font-medium text-blue-300"
                        >
                          {tech}
                        </span>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        {/* --- */}

        {/* Skills Section */}
        <section id="skills" className="py-20 px-4 bg-slate-900/70">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold mb-12 text-center text-gray-200">
              <span className="border-b-4 border-blue-500 pb-1">Technical Skills</span>
            </h2>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[
                { title: 'Programming Languages', skills: skills.languages },
                { title: 'Frontend Development', skills: skills.frontend },
                { title: 'Backend & Frameworks', skills: skills.backend },
                { title: 'Databases', skills: skills.databases },
                { title: 'AI / Machine Learning', skills: skills.aiml },
                { title: 'Tools, DevOps & Cloud', skills: skills.tools }
              ].map((category, index) => (
                <div 
                  key={category.title}
                  className="bg-slate-900/50 backdrop-blur-sm rounded-xl p-6 border border-slate-800 hover:border-purple-500 transition-all duration-300 transform hover:scale-[1.02] hover:shadow-lg hover:shadow-purple-900/30 animate-slide-in-up"
                  style={{ animationDelay: `${index * 0.1}s` }}
                >
                  <h3 className="text-xl font-bold mb-4 text-purple-400 flex items-center gap-3">
                    <Code className="w-5 h-5 text-blue-400" />
                    {category.title}
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {category.skills.map((skill, i) => (
                      <span 
                        key={i} 
                        className="px-3 py-1 bg-purple-900/30 rounded-full text-sm font-medium text-purple-300 hover:bg-purple-800/50 transition-colors duration-200"
                      >
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            <div id="activities" className="mt-12 bg-slate-900/50 backdrop-blur-sm rounded-xl p-8 border border-slate-800 hover:border-yellow-500/50 transition-all duration-300 hover:shadow-2xl hover:shadow-yellow-900/30 animate-slide-in-up" style={{ animationDelay: '0.7s' }}>
                <h3 className="text-2xl font-bold mb-6 text-center text-white flex items-center justify-center gap-3">
                    <Award className="w-6 h-6 text-yellow-400" />
                    Certifications & Activities
                </h3>
                <div className="grid md:grid-cols-2 gap-4">
                    {[
                        'Agile Scrum in Practice – Infosys',
                        'Programming in Python – KIMO',
                        'AWS Certified (Backend Infrastructure) - Self-Study',
                        'Cyber Job Simulation – Deloitte',
                        'Training on DSA with JAVA – Talent Battle',
                        'Workshop on Data Science with Python'
                    ].map((cert, i) => (
                        <div 
                            key={i} 
                            className="flex items-center gap-2 text-gray-300 hover:text-yellow-400 transition-colors duration-300 text-base"
                        >
                            <span className="text-yellow-400 flex-shrink-0">★</span>
                            <span>{cert}</span>
                        </div>
                    ))}
                </div>
                {/* Simplified Activities into a cleaner highlight box for a professional resume feel */}
                <div className="mt-8 border-t border-slate-800 pt-6">
                    <h4 className="text-xl font-bold text-blue-400 mb-4">Leadership & Hackathons:</h4>
                    {activities.map((act, index) => (
                        <div key={index} className="mb-4">
                            <p className="font-semibold text-white flex items-center gap-2">
                                <span className="text-blue-300 flex-shrink-0">○</span> 
                                {act.title} at {act.organization} ({act.period})
                            </p>
                            <ul className="list-disc list-inside ml-4 text-gray-400 text-sm">
                                {act.points.map((point, i) => (
                                    <li key={i}>{point}</li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </div>
            </div>
          </div>
        </section>

        {/* --- */}

        {/* Contact Section */}
        <section id="contact" className="py-20 px-4 bg-slate-900/50">
          <div className="max-w-4xl mx-auto text-center animate-slide-in-up">
            <h2 className="text-4xl font-bold mb-4 text-white">
              <span className="border-b-4 border-blue-500 pb-1">Get In Touch</span>
            </h2>
            <p className="text-xl text-gray-300 mb-10 max-w-2xl mx-auto">
              I'm always open to discussing new projects, collaboration opportunities, or technical challenges. Feel free to reach out.
            </p>
            <div className="flex flex-wrap justify-center gap-6">
              <a href="mailto:hemanthk.cse.rymec@gmail.com" className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 px-7 py-3 rounded-lg font-semibold transition-all duration-300 transform hover:scale-105 shadow-xl shadow-blue-500/20">
                <Mail className="w-5 h-5" />
                Email Me
              </a>
              <a href="https://www.linkedin.com/in/kuruba-hemanth-kishore/" target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 border border-gray-700 hover:border-purple-500 px-7 py-3 rounded-lg font-semibold text-gray-300 hover:text-purple-400 transition-all duration-300 transform hover:scale-105">
                <Linkedin className="w-5 h-5" />
                LinkedIn
              </a>
              <a href="https://github.com/yourusername" target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 border border-gray-700 hover:border-pink-500 px-7 py-3 rounded-lg font-semibold text-gray-300 hover:text-pink-400 transition-all duration-300 transform hover:scale-105">
                <Github className="w-5 h-5" />
                GitHub
              </a>
            </div>
          </div>
        </section>

        {/* --- */}

        {/* Footer */}
        <footer className="py-6 px-4 border-t border-slate-800 bg-slate-950">
          <div className="max-w-6xl mx-auto text-center text-gray-500 text-sm">
            <p className="mb-2">Built with React & Tailwind CSS. Design inspired by modern development portfolios.</p>
            <p className="text-white font-medium">© {new Date().getFullYear()} Kuruba Hemanth Kishore</p>
          </div>
        </footer>
      </main>
    </div>
  );
}