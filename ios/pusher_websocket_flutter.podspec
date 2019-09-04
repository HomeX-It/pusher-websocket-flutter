#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'pusher_websocket_flutter'
  s.version          = '0.1.1'
  s.summary          = 'A new flutter plugin project.'
  s.description      = <<-DESC
Pusher Flutter Client
                       DESC
  s.homepage         = 'https://github.com/HomeXLabs/pusher-websocket-flutter'
  s.license          = { :file => '../../LICENSE' }
  s.author           = { 'HomeX' => 'jrai@homex.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.swift_version = '4.2'
  s.dependency 'Flutter'
  s.dependency 'PusherSwift', '7.0.0'

  s.ios.deployment_target = '9.0'
end

